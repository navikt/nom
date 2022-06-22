package no.nav.nom.apps.navident.rnrservices;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.IdentService;
import no.nav.nom.apps.navident.domain.IdentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class IdentServiceImpl implements IdentService {
    private Map<String, CountDownLatch> requests = new ConcurrentHashMap<>();
    private Map<String, Respons> responses = new ConcurrentHashMap<>();

    @Autowired NeedProducer needProducer;

    public Respons bestillIdent(char idPrefix, String personid, IdentType type)  {
        needProducer.produceNeed(idPrefix, personid, type);
        CountDownLatch latch = new CountDownLatch(1);
        requests.put(personid, latch);

        try {
            if (latch.await(1, TimeUnit.SECONDS)) {
                if (responses.containsKey(personid)) {
                    Respons respons = responses.get(personid);
                    responses.remove(respons);
                    log.debug("Returnerer respons for personid %s : %s".formatted(mask(personid), respons));
                    return respons;
                } else {
                    log.error("Forventet respons data for bestilling av personid %s, men ingen var tilgjengelig".formatted(mask(personid)));
                    return Respons.builder()
                            .code(500)
                            .body(Respons.Body.builder()
                                    .feilmelding("Noe gikk feil i ident bestilling. Resultat ikke tilgjengelig")
                                    .build())
                            .build();
                }
            } else { // TimeOut
                log.error("Tjenesten for bestilling av personid timet ut i bestilling av %s".formatted(mask(personid)));
                return Respons.builder()
                        .code(500)
                        .body(Respons.Body.builder()
                                .feilmelding("Behandling av ident bestilling timet ut")
                                .build())
                        .build();
            }
        } catch (InterruptedException e) {
            log.error("Ident bestilling for ident %s ble avbrutt".formatted(mask(personid)), e);
            return Respons.builder()
                    .code(500)
                    .body(Respons.Body.builder()
                            .feilmelding("Kunne ikke prosessere ident bestilling. Årsak: %s".formatted(e.getMessage()))
                            .build())
                    .build();
        }
    }



    @KafkaListener(
            id = "IdentService",
            topics = "${app.kafka.topic.rnr.name}")
    public void listen(Need need) {
        if (!need.isResponsProdusentVisited()) return;

        Respons respons = need.getRespons();
        responses.put(need.getPersonid(), respons);

        CountDownLatch latch =
                requests.get(need.getPersonid());

        if (latch != null) latch.countDown();
    }


    private String mask(String toBeMasked) {
        if (toBeMasked == null || toBeMasked.length() < 7) return toBeMasked;
        return toBeMasked.substring(0, 6) + "***";
    }
}