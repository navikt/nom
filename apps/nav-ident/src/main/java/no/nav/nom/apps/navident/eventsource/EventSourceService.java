package no.nav.nom.apps.navident.eventsource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.domain.Personid;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.Arrays;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class EventSourceService {
    private final Map<String, Navident> navidenter;

    @KafkaListener(
            id = "EventSourceListener",
            topics = "${app.kafka.topic.eventsource.name}")
    public void listen(SourceEvent event) {
        log.debug(event.maskedToString());

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


    private void handleCreateEvent(SourceEvent event) {
        if ( navidenter.values().stream().anyMatch( id -> id.is(event.payload.personid))) {
            throw new IllegalSourceEventException("Personid id i OPPRETT event for navident %s finnes ifra før en på annen navident. Kan ikke behandle OPPRETT SourceEvent", event.payload.navident);
        }

        Personid personid = Personid
                .builder()
                .id(event.payload.personid)
                .type(event.payload.personidtype)
                .build();

        Navident ident = Navident
                .builder()
                .navident(event.payload.navident)
                .gjeldendePersonid(personid.getId())
                .identType(event.payload.identType)
                .allePersonider(Arrays.asList(personid))
                .build();

        if (navidenter.containsKey(event.payload.navident)) {
            log.info("Navident %s finnes ifra før. Denne eventen erstatter tidligere info", event.payload.navident);
        }

        navidenter.put(event.payload.navident, ident);

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
}
