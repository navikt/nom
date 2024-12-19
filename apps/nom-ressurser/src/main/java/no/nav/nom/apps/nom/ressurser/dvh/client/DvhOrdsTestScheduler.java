package no.nav.nom.apps.nom.ressurser.dvh.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")
public class DvhOrdsTestScheduler {
    private final DvhOrdsClient dvhOrdsClient;
    private final long DELAY = 60*1000;

    public DvhOrdsTestScheduler(
        @Value("${dvh.endpoint}") String dvhEndpoint,
         DvhOrdsClient dvhOrdsClient
    ) {
        this.dvhOrdsClient = dvhOrdsClient;
    }

//    @Scheduled(fixedDelay = DELAY)
    public void dvhScheduler(){
        testOrds();
    }

    private void testOrds() {
        try {
            log.info("Test av ORDS");
            var singleRes = dvhOrdsClient.dvhGetJsonDataLimitRows(1);
            var ressurs = singleRes.get("items").get(0);
            log.info("Test av ORDS - hentet rad med : {}: {} - {} [{} - {}]",
                ressurs.get("pk_hrres_nom_historikk"),
                ressurs.get("nav_id"),
                ressurs.get("fornavn"),
                ressurs.get("remedy_start_dato"),
                ressurs.get("remedy_slutt_dato"));

            var parsedResult = dvhOrdsClient.dvhGetDataLimitRows(1);
            var parsedRessurs = parsedResult.getAnsatteResultList().getFirst();
            log.info("Test av ORDS - parsed rad med : {}: {} - {} [{} - {}]",
                parsedRessurs.getOffset(),
                parsedRessurs.getNavident(),
                parsedRessurs.getFornavn(),
                parsedRessurs.getRemedyStartDato(),
                parsedRessurs.getRemedySluttDato());

            var testSekvens = dvhOrdsClient.dvhGetDataInSekvens(417823, 417843);
            log.info("Test av ORDS - lest sekvens [417823 - 417843] - antall lest: {}  (burde være 20 stk)", testSekvens.getAnsatteResultList().size());
            log.info("Test av ORDS - lest sekvens [417823 - 417843] - første pk: {}  siste pk: {}",
                testSekvens.getAnsatteResultList().getFirst().getOffset(),
                testSekvens.getAnsatteResultList().getLast().getOffset());
        } catch (Exception e) {
            log.error("Feil ved test av ORDS", e);
        }
    }
}
