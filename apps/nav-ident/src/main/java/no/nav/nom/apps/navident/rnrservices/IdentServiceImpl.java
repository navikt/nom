package no.nav.nom.apps.navident.rnrservices;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.IdentService;
import no.nav.nom.apps.navident.common.cache.RnRMdcCache;
import no.nav.nom.apps.navident.domain.IdentType;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class IdentServiceImpl implements IdentService, ConsumerSeekAware {
    // Map contents need to be consistent between thread running bestillIdent(...) and thread listening for the RnR Response
    private Map<String, CountDownLatch> requests = Collections.synchronizedMap(new HashMap<>());
    private Map<String, Respons> responses = Collections.synchronizedMap(new HashMap<>());

    @Autowired RnRMdcCache rnRMdcCache;
    @Autowired NeedProducer needProducer;

    public Respons bestillIdent(String personid, IdentType type, char idPrefix)  {
        log.debug("bestillIdent(personId = %s, type = %s, idPrefix = '%s') invokert".formatted(mask(personid), type, idPrefix));
        CountDownLatch latch = new CountDownLatch(1);
        /**
         * Thread safe consideration.
         * This block needs to complete before any Response listener tries to fetch the latch from map
         */
        String uuid = null;
        synchronized (this) {
            uuid = needProducer.produceNeed(idPrefix, personid, type);
            requests.put(uuid, latch);
        }

        try {
            if (latch.await(5, TimeUnit.SECONDS)) { // First time after pod restart is usually slow.
                if (responses.containsKey(uuid)) {
                    Respons respons = responses.get(uuid);
                    responses.remove(uuid);
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
            id = "${app.kafka.identServiceListenerName}",
            topics = "${app.kafka.topic.rnr.name}")
    public void listen(Need need) {
        if (!need.isResponsProdusentVisited()) return;

        rnRMdcCache.loadMdc(need.getUuid());

        Respons respons = need.getRespons();
        responses.put(need.getUuid(), respons);

        /**
         * Thread safe consideration
         * Do not try to find a latch unless bestillIdent threads has fully completed both to produceNeed and put latch onto the map
         */
        synchronized (this) {
            CountDownLatch latch = requests.get(need.getUuid());

            if (latch != null) latch.countDown();
            if (latch == null)
                log.error("Got final rnr ResponseProducer=visited message, but latch is not present in the request map. I thought this would not be possible when synchronized.. :(");
        }

        MDC.clear();
    }

    @Value("${app.kafka.identServiceListenerName}") private String listenerName;

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        final List<String> topics = assignments.keySet().stream().map( p -> p.topic() ).collect(Collectors.toList());
        log.info("Kafka partions on topics %s assigned to listener %s. Seek to end in order to only process new messages".formatted(topics, listenerName));
        callback.seekToEnd(assignments.keySet());
    }

    private String mask(String toBeMasked) {
        if (toBeMasked == null || toBeMasked.length() < 7) return toBeMasked;
        return toBeMasked.substring(0, 6) + "***";
    }
}