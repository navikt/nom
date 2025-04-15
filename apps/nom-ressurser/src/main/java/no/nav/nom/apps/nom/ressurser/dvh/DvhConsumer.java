package no.nav.nom.apps.nom.ressurser.dvh;

import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.nom.ressurser.dvh.client.DvhOrdsClient;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("!dev & !local & !test")
public class DvhConsumer {
    private final DvhConsumerProcessorService dvhConsumerProcessorService;
    private final OffsetRepository offsetRepository;
    private final LeaderElection leaderElection;
    private final Counter dvhReadCounter;
    private final DvhOrdsClient dvhOrdsClient;

    private final long incrementValue = 1000L;
    private final long DELAY = 10*60*1000; // 10 minutter

    public DvhConsumer(
        DvhConsumerProcessorService dvhConsumerProcessorService,
        OffsetRepository offsetRepository,
        LeaderElection leaderElection,
        Counter dvhReadCounter, DvhOrdsClient dvhOrdsClient
    ) {
        this.dvhConsumerProcessorService = dvhConsumerProcessorService;
        this.offsetRepository = offsetRepository;
        this.leaderElection = leaderElection;
        this.dvhReadCounter = dvhReadCounter;
        this.dvhOrdsClient = dvhOrdsClient;
    }

    @Scheduled(initialDelay = 180000, fixedDelay = DELAY*30) // utvidet delay med 5 timer
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

        var result = dvhOrdsClient.dvhGetDataInSekvens(dvhOffset.getOffset(), dvhOffset.getOffset() + incrementValue);

        if (result != null && result.getAnsatteResultList() != null && !result.getAnsatteResultList().isEmpty()) {
            val resultList = result.getAnsatteResultList();
            dvhConsumerProcessorService.processRessurser(resultList);

            dvhOffset.setOffset(resultList.get(resultList.size() - 1).getOffset());
            offsetRepository.save(dvhOffset);

            dvhReadCounter.increment(resultList.size());
            return resultList.size();
        }
        return 0;
    }
}
