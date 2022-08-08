package no.nav.nom.apps.navident.eventsource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.Navident;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

import java.util.Arrays;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class EventSourceService {

    @Value("${app.kafka.eventSourceListenerName}") public final String listenerId;
    private final Map<String, Navident> navidenter;

    private boolean hasReadToEndOnStartup = false;

    @KafkaListener(
            id = "${app.kafka.eventSourceListenerName}",
            topics = "${app.kafka.topic.eventsource.name}")
    public void listen(SourceEvent event, @Header(KafkaHeaders.OFFSET) long currentOffset) {
        log.debug("Got SourceEvent on offset %d: %s".formatted(currentOffset, event.maskedToString()));

        try {
            // Update domain model with sourced event
            switch (event.event) {
                case OPPRETT -> handleCreateEvent(event);   // Knytning lages mellom personid og navident
                case SLETT -> handleDeleteEvent(event);     // Knytning skal slettes og navident vil ikke være tilgjengelig mer i senere bestillinger. Administrativ funksjon
                default -> log.error("Unknown event type %s".formatted(event.event));
            }
        } catch (IllegalSourceEventException e) {
            String error = String.format("Could not handle SourceEvent. Reason: %s", e.getMessage());
            log.error(error);
            log.debug("Callstack:", e);
        }
    }

    public boolean hasAllSourceEventsBeenRead() {
        return hasReadToEndOnStartup;
    }

    private void handleCreateEvent(SourceEvent event) {
        String navid =  event.payload.navident;
        String personid = event.payload.personid;
        if ( navidenter.values().stream().anyMatch( id -> id.is(personid) && navid != id.getNavident() ) ) {
            throw new IllegalSourceEventException("Personid id i OPPRETT event for navident %s finnes ifra før en på annen navident. Kan ikke behandle OPPRETT SourceEvent", navid);
        }

        Navident ident = null;
        if (navidenter.containsKey(navid)) {
            log.info("Navident %s finnes ifra før. Denne eventen erstatter tidligere info", navid);

            ident = navidenter.get(navid).clone();
            ident.setGjeldendePersonid(personid);  // ASSUMPTION!! Siste personid i eventsource benyttes som gjeldende
            if (!ident.getAllePersonider().contains(personid)) {
                ident.addPersonid(personid);
            } else {
                log.info("Personid finnes ifra før på denne navidenten %s. Denne eventen endrer kanskje kun på gjeldendeIdent info", navid);
            }
        } else {
            ident = Navident
                    .builder()
                    .navident(navid)
                    .gjeldendePersonid(event.payload.personid)
                    .identType(event.payload.identType)
                    .allePersonider(Arrays.asList(event.payload.personid))
                    .build();
        }

        navidenter.put(navid, ident);
        log.debug(ident.maskedToString());
    }

    private void handleDeleteEvent(SourceEvent event) {
        String navident = event.payload.navident;
        if (!navidenter.containsKey(navident)) {
            throw new IllegalSourceEventException("Finner ingen NavIdent %s. Kan ikke behandle DELETE SourceEvent", navident);
        }
        navidenter.remove(navident);
        log.debug("Fjernet navident %s fra domain".formatted(navident));
    }

    @EventListener
    public void eventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup) return;

        log.debug("Idle event received for listener %s. Waiting for listener %s".formatted(event.getListenerId(), listenerId));
        if (event.getListenerId().startsWith(listenerId)) {
            log.info("All SourceEvents has now been read on startup");
            hasReadToEndOnStartup = true;
        }
    }
}
