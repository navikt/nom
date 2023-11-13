package no.nav.nom.apps.navident.rnrservices;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.IdentService;
import no.nav.nom.apps.navident.common.LogConfig;
import no.nav.nom.apps.navident.common.cache.RedisSynchronizationCache;
import no.nav.nom.apps.navident.common.cache.RnRMdcCache;
import no.nav.nom.contract.nom.IdentType;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class IdentServiceImpl implements IdentService, ConsumerSeekAware {
    // Map contents need to be consistent between thread running bestillIdent(...) and thread listening for the RnR Response
    private final Map<String, CountDownLatch> requests = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Respons> responses = Collections.synchronizedMap(new HashMap<>());

    private final RnRMdcCache rnRMdcCache;
    private final NeedProducer needProducer;
    private final RedisSynchronizationCache redisSynchronizationCache;

    public IdentServiceImpl(RnRMdcCache rnRMdcCache, NeedProducer needProducer, RedisSynchronizationCache redisSynchronizationCache) {
        this.rnRMdcCache = rnRMdcCache;
        this.needProducer = needProducer;
        this.redisSynchronizationCache = redisSynchronizationCache;
    }

    public Respons bestillIdent(String personid, IdentType type, char idPrefix)  {
        log.debug(LogConfig.SECURE_LOGS, "bestillIdent(personId = {}, type = {}, idPrefix = '{}') invokert", personid, type, idPrefix);
        log.debug("bestillIdent(personId = %s, type = %s, idPrefix = '%s') invokert".formatted(mask(personid), type, idPrefix));

        if (!redisSynchronizationCache.tryLock(personid)) {
            // Some other process has lock on this personIdent. Return error response
            log.debug(LogConfig.SECURE_LOGS, "Unable to obtain lock for personident %s".formatted(personid));
            log.debug("Unable to obtain lock for personident %s".formatted(mask(personid)));
            return Respons.builder()
                .httpStatus(HttpStatus.CONFLICT)
                .body(Respons.Body.builder()
                    .feilmelding("Kall til bestillIdent for angitt personIdent er allerede i process")
                    .build())
                .build();
        }

        CountDownLatch latch = new CountDownLatch(1);
        /*
         * Thread safe consideration.
         * This block needs to complete before any Response listener tries to fetch the latch from map
         */
        String uuid = null;
        synchronized (this) {
            uuid = needProducer.produceNeed(idPrefix, personid, type);
            requests.put(uuid, latch);
        }

        try {
            if (latch.await(15, TimeUnit.SECONDS)) { // Often get 4-6 second delay on cold runs.
                redisSynchronizationCache.unlock(personid);

                if (responses.containsKey(uuid)) {
                    Respons respons = responses.get(uuid);
                    responses.remove(uuid);
                    log.debug(LogConfig.SECURE_LOGS, "Returnerer respons for personid {} : {}", personid, respons);
                    log.debug("Returnerer respons for personid %s : %s".formatted(mask(personid), respons));
                    return respons;
                } else {
                    log.error(LogConfig.SECURE_LOGS, "Forventet respons data for bestilling av personid {}, men ingen var tilgjengelig", personid);
                    log.error("Forventet respons data for bestilling av personid %s, men ingen var tilgjengelig".formatted(mask(personid)));
                    return Respons.builder()
                            .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Respons.Body.builder()
                                    .feilmelding("Noe gikk feil i ident bestilling. Resultat ikke tilgjengelig")
                                    .build())
                            .build();
                }
            } else { // TimeOut
                redisSynchronizationCache.unlock(personid);

                log.error(LogConfig.SECURE_LOGS, "Tjenesten for bestilling av personid timet ut i bestilling av {}", personid);
                log.error("Tjenesten for bestilling av personid timet ut i bestilling av %s".formatted(mask(personid)));
                return Respons.builder()
                        .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Respons.Body.builder()
                                .feilmelding("Behandling av ident bestilling timet ut")
                                .build())
                        .build();
            }
        } catch (InterruptedException e) {
            redisSynchronizationCache.unlock(personid);

            log.error(LogConfig.SECURE_LOGS, "Ident bestilling for ident {} ble avbrutt", personid, e);
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

        /*
         * Thread safe consideration
         * Do not try to find a latch unless bestillIdent threads has fully completed both to produceNeed and put latch onto the map
         */
        synchronized (this) {
            CountDownLatch latch = requests.get(need.getUuid());

            if (latch != null) latch.countDown();
            if (latch == null)
                log.debug("Got final rnr ResponseProducer=visited message, but latch is not present in the request map. Got to be in a different pod - ignoring");
        }

        MDC.clear();
    }

    @Value("${app.kafka.identServiceListenerName}") private String listenerName;

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        final List<String> topics = assignments.keySet().stream().map(TopicPartition::topic).collect(Collectors.toList());
        log.info("Kafka partions on topics %s assigned to listener %s. Seek to end in order to only process new messages".formatted(topics, listenerName));
        callback.seekToEnd(assignments.keySet());
    }

    private String mask(String toBeMasked) {
        if (toBeMasked == null || toBeMasked.length() < 7) return toBeMasked;
        return toBeMasked.substring(0, 6) + "***";
    }
}