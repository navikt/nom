package no.nav.nom.apps.nom.enheter.dvh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.nom.enheter.common.LeaderElection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!local & !test")
public class DvhConsumer {

    @Value("${dvh.endpoint}")
    private String dvhEndpoint;

    private final OffsetRepository offsetRepository;
    private final LeaderElection leaderElection;
    private final DvhConsumerProcessorService dvhConsumerProcessorService;

    private final RestTemplate restTemplate = new RestTemplate();

    private final long DELAY = 60*60*1000;

    @Scheduled(initialDelay = 180000, fixedDelay = DELAY)
    @Transactional(value = "transactionManager")
    public void dvhScheduler(){
        if(leaderElection.isLeader()) {
            log.info("Oppdaterer fra datavarehus..");
            val dvhOffset = offsetRepository.findByOffsetName("DVH_OFFSET");
            val url = new StringBuilder(dvhEndpoint)
                    .append("?in_sekvens_fra=")
                    .append(dvhOffset.getOffset())
                    .toString();

            log.info("Leser fra sekvens {}", dvhOffset.getOffset());
            DvhResultWrapper result = restTemplate.getForObject(url, DvhResultWrapper.class);

            if (result != null && !result.getEnhetResultList().isEmpty()) {
                val resultList = result.getEnhetResultList();
                resultList.sort(Comparator.comparingLong(DvhEnhetDTO::getOffset));
                dvhConsumerProcessorService.processResult(resultList);

                dvhOffset.setOffset(resultList.get(resultList.size() - 1).getOffset());
                offsetRepository.save(dvhOffset);

            }
            log.info("Oppdatering fra datavarehus ferdig.");
        }
    }
}
