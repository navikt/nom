package no.nav.nom.apps.navident.rnrservices;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.IdentService;
import no.nav.nom.apps.navident.domain.IdentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    public Respons bestillIdent(String personid, IdentType type, char idPrefix)  {
        String uuid = needProducer.produceNeed(idPrefix, personid, type);
        CountDownLatch latch = new CountDownLatch(1);
        requests.put(uuid, latch);

        try {
            if (latch.await(1, TimeUnit.SECONDS)) {
                if (responses.containsKey(uuid)) {
                    Respons respons = responses.get(uuid);
                    responses.remove(respons);
                    log.debug("Returnerer respons for personid %s : %s".formatted(mask(personid), respons));
                    return respons;
                } else {
                    log.error("Forventet respons data for bestilling av personid %s, men ingen var tilgjengelig".formatted(mask(personid)));
                    return Respons.builder()
                            .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Respons.Body.builder()
                                    .feilmelding("Noe gikk feil i ident bestilling. Resultat ikke tilgjengelig")
                                    .build())
                            .build();
                }
            } else { // TimeOut
                log.error("Tjenesten for bestilling av personid timet ut i bestilling av %s".formatted(mask(personid)));
                return Respons.builder()
                        .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Respons.Body.builder()
                                .feilmelding("Behandling av ident bestilling timet ut")
                                .build())
                        .build();
            }
        } catch (InterruptedException e) {
            log.error("Ident bestilling for ident %s ble avbrutt".formatted(mask(personid)), e);
            return Respons.builder()
                    .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
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
        responses.put(need.getUuid(), respons);

        CountDownLatch latch =
                requests.get(need.getUuid());

        if (latch != null) latch.countDown();
    }


    private String mask(String toBeMasked) {
        if (toBeMasked == null || toBeMasked.length() < 7) return toBeMasked;
        return toBeMasked.substring(0, 6) + "***";
    }
}