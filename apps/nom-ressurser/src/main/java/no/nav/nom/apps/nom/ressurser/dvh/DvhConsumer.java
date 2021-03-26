package no.nav.nom.apps.nom.ressurser.dvh;

import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!local & !test")
public class DvhConsumer {

    @Value("${dvh.endpoint}")
    private String dvhEndpoint;

    private final DvhConsumerProcessorService dvhConsumerProcessorService;
    private final OffsetRepository offsetRepository;
    private final LeaderElection leaderElection;
    private final Counter dvhReadCounter;

    private final RestTemplate restTemplate = new RestTemplate();
    private final long incrementValue = 1000L;

    private final long DELAY = 60*60*1000;

    @Scheduled(initialDelay = 180000, fixedDelay = DELAY)
    @Transactional(value = "transactionManager")
    public void dvhScheduler(){
        if(leaderElection.isLeader()) {
            log.info("Oppdaterer fra datavarehus..");
            while (hentBulkOppdatering() == incrementValue);
            log.info("Oppdatering fra datavarehus ferdig.");
        }
    }

    public int hentBulkOppdatering(){
        val dvhOffset = offsetRepository.findByOffsetName("DVH_OFFSET");
        val url = new StringBuilder(dvhEndpoint)
                .append("?in_sekvensnummer_fra=")
                .append(dvhOffset.getOffset())
                .append("&in_sekvensnummer_til=")
                .append(dvhOffset.getOffset() + incrementValue)
                .toString();

        log.info("Leser fra sekvens {} til sekvens {}", dvhOffset.getOffset(), dvhOffset.getOffset() + incrementValue);
        DvhResponseWrapper result = restTemplate.getForObject(url, DvhResponseWrapper.class);

        if (result != null && result.getAnsattResultList() != null && !result.getAnsattResultList().isEmpty()) {
            val resultList = result.getAnsattResultList();
            dvhConsumerProcessorService.processRessurser(resultList);

            dvhOffset.setOffset(resultList.get(resultList.size() - 1).getOffset());
            offsetRepository.save(dvhOffset);

            dvhReadCounter.increment(resultList.size());
            return resultList.size();
        }
        return 0;
    }
}
