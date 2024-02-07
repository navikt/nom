package no.nav.nom.apps.nom.ressurser.dvh;

import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@Profile("!dev & !local & !test")
public class DvhConsumer {

    private final DvhConsumerProcessorService dvhConsumerProcessorService;
    private final OffsetRepository offsetRepository;
    private final LeaderElection leaderElection;
    private final Counter dvhReadCounter;

    private final WebClient webClient;

    private final long incrementValue = 1000L;

    private final long DELAY = 60*1000;

    public DvhConsumer(
        @Value("${dvh.endpoint}") String dvhEndpoint,
        DvhConsumerProcessorService dvhConsumerProcessorService,
        OffsetRepository offsetRepository,
        LeaderElection leaderElection,
        Counter dvhReadCounter
    ) {
        this.dvhConsumerProcessorService = dvhConsumerProcessorService;
        this.offsetRepository = offsetRepository;
        this.leaderElection = leaderElection;
        this.dvhReadCounter = dvhReadCounter;

        this.webClient = WebClient.builder()
            .exchangeStrategies(
                ExchangeStrategies.builder()
                    .codecs(codecConfigurer -> codecConfigurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                    .build())
            .baseUrl(dvhEndpoint)
            .build();
    }

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

        log.info("Leser fra sekvens {} til sekvens {}", dvhOffset.getOffset(), dvhOffset.getOffset() + incrementValue);
        DvhResponseWrapper result = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("in_sekvensnummer_fra", dvhOffset.getOffset())
                .queryParam("in_sekvensnummer_til", dvhOffset.getOffset() + incrementValue)
                .build()
            )
            .retrieve()
            .bodyToMono(DvhResponseWrapper.class)
            .block();

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
