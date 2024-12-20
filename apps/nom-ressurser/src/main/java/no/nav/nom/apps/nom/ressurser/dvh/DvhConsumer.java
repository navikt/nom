package no.nav.nom.apps.nom.ressurser.dvh;

import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.nom.ressurser.dvh.client.DvhOrdsClient;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("!dev & !local & !test")
public class DvhConsumer {
    private final DvhConsumerProcessorService dvhConsumerProcessorService;
    private final OffsetRepository offsetRepository;
    private final LeaderElection leaderElection;
    private final Counter dvhReadCounter;

    private final WebClient webClient;
    private final DvhOrdsClient dvhOrdsClient;

    private final long incrementValue = 1000L;

    private final long DELAY = 10*60*1000; // 10 minutter

    public DvhConsumer(
        @Value("${dvh.endpoint}") String dvhEndpoint,
        DvhConsumerProcessorService dvhConsumerProcessorService,
        OffsetRepository offsetRepository,
        LeaderElection leaderElection,
        Counter dvhReadCounter, DvhOrdsClient dvhOrdsClient
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
        this.dvhOrdsClient = dvhOrdsClient;
    }

    @Scheduled(initialDelay = 180000, fixedDelay = DELAY)
    @Transactional(value = "transactionManager")
    public void dvhScheduler(){
//        testOrds();
        if(leaderElection.isLeader()) {
            log.info("Oppdaterer fra datavarehus..");
            while (hentBulkOppdatering() == incrementValue);
            log.info("Oppdatering fra datavarehus ferdig.");
        }
    }

    private void testOrds() {
        try {
            log.info("Tester av ORDS på oppstart");
            var singleRes = dvhOrdsClient.dvhGetJsonDataLimitRows(1);
            var ressurs = singleRes.get("items").get(0);
            log.info("Test av ORDS på oppstart - hentet rad med : {} - {} [{} - {}]",
                ressurs.get("nav_id"),
                ressurs.get("navn"),
                ressurs.get("remedy_start_dato"),
                ressurs.get("remedy_slutt_dato"));
        } catch (Exception e) {
            log.error("Feil ved test av ORDS i oppstart", e);
        }
    }

    public int hentBulkOppdatering(){
        val dvhOffset = offsetRepository.findByOffsetName("DVH_OFFSET");

        log.info("Leser fra sekvens {} til sekvens {}", dvhOffset.getOffset(), dvhOffset.getOffset() + incrementValue);
//        DvhResponseWrapper result = webClient.get()
//            .uri(uriBuilder -> uriBuilder
//                .queryParam("in_sekvensnummer_fra", dvhOffset.getOffset())
//                .queryParam("in_sekvensnummer_til", dvhOffset.getOffset() + incrementValue)
//                .build()
//            )
//            .retrieve()
//            .bodyToMono(DvhResponseWrapper.class)
//            .block();

        var result = dvhOrdsClient.dvhGetDataInSekvens(dvhOffset.getOffset(), dvhOffset.getOffset() + incrementValue);

        if (result != null && result.getAnsatteResultList() != null && !result.getAnsatteResultList().isEmpty()) {
//            compareTdvWithOrds(result, ordsResult);

            val resultList = result.getAnsatteResultList();
            dvhConsumerProcessorService.processRessurser(resultList);

            dvhOffset.setOffset(resultList.get(resultList.size() - 1).getOffset());
            offsetRepository.save(dvhOffset);

            dvhReadCounter.increment(resultList.size());
            return resultList.size();
        }
        return 0;
    }

    private void compareTdvWithOrds(DvhResponseWrapper tdvResult, DvhOrdsResponseWrapper ordsResult) {
        try {
            var tdvAnsatte = tdvResult.getAnsattResultList();
            var ordsAnsatte = ordsResult.getAnsatteResultList();

            boolean erLikt = true;
            if (tdvAnsatte.size() != ordsAnsatte.size()) {
                erLikt = false;
                log.error("Feil ved sammenligning av TDV og ORDS, antall rader er ulike - {} vs {}", tdvAnsatte.size(), ordsAnsatte.size());
            }

            var ordsMap = ordsAnsatte.stream().collect(Collectors.toMap(DvhRessursDTO::getNavident, Function.identity()));
            int idxSkip = tdvAnsatte.size()/20;
            for (int i = 0; i < 20; i++) {
                int idx = i * idxSkip;
                var dvhRessursTdv = tdvAnsatte.get(idx);
                var dvhRessursOrds = ordsMap.get(dvhRessursTdv.getNavident());
                if (dvhRessursOrds == null || !compare(dvhRessursTdv, dvhRessursOrds)) {
                    log.error("Ressurs fra TDV og ORDS er ulik. \n\tTDV: \n\t{}, \n\tORDS: \n\t{}"
                        , dvhRessursTdv.toString()
                        , dvhRessursOrds == null ? null : dvhRessursOrds.toString());
                    erLikt = false;
                }
            }
            if (erLikt) {
                log.info("Last av ressurser fra ORDS (nom_ords_ansatte_buckets.json) er lik last fra TDV (NOM_ANSATTE_BUCKETS_v2)");
            } else {
                log.error("Last av ressurser fra ORDS (nom_ords_ansatte_buckets.json) er annerledes enn last fra TDV (NOM_ANSATTE_BUCKETS_v2)");
            }
        } catch (Exception e) {
            log.error("Feil ved sammenligning av TDV og ORDS", e);
        }
    }

    private boolean compare(DvhRessursTdvDTO dvhRessursTdv, DvhOrdsRessursDTO dvhRessursOrds) {
        return Objects.equals(dvhRessursTdv.getNavident(), dvhRessursOrds.getNavident()) &&
            Objects.equals(dvhRessursTdv.getFornavn(), dvhRessursOrds.getFornavn()) &&
            Objects.equals(dvhRessursTdv.getEtternavn(), dvhRessursOrds.getEtternavn()) &&
            Objects.equals(dvhRessursTdv.getEpost(), dvhRessursOrds.getEpost()) &&
            Objects.equals(dvhRessursTdv.getRessursTypeKode(), dvhRessursOrds.getRessursTypeKode()) &&
            Objects.equals(dvhRessursTdv.getAnsattType(), dvhRessursOrds.getAnsattType()) &&
            Objects.equals(dvhRessursTdv.getPermisjon(), dvhRessursOrds.getPermisjon()) &&
            Objects.equals(dvhRessursTdv.getRemedyStartDato(), dvhRessursOrds.getRemedyStartDato()) &&
            Objects.equals(dvhRessursTdv.getRemedySluttDato(), dvhRessursOrds.getRemedySluttDato());
    }
}
