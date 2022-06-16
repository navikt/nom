package no.nav.nom.apps.navident.eventsource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.domain.Periode;
import no.nav.nom.apps.navident.domain.Personid;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                case OPPRETT -> handleCreateEvent(event);
                case DEAKTIVER -> handleDeactivateEvent(event);
                case REAKTIVER -> handleReactivateEvent(event);
                case SLETT -> handleDeleteEvent(event);
                default -> log.error("Unknown event type %s".formatted(event.event));
            }
        } catch (IllegalSourceEventException e) {
            String error = String.format("Could not handle SourceEvent. Reason: %s", e.getMessage());
            log.error(error);
            log.debug("Callstack:", e);
        }
    }


    private void handleCreateEvent(SourceEvent event) {
        if (navidenter.containsKey(event.payload.navident)) {
            throw new IllegalSourceEventException("Navident %s finnes ifra før. Kan ikke behandle OPPRETT SourceEvent", event.payload.navident);
        }
        if ( navidenter.values().stream().anyMatch( id -> id.is(event.payload.personid))) {
            throw new IllegalSourceEventException("Personid id i OPPRETT event for navident %s finnes ifra før en på annen navident. Kan ikke behandle OPPRETT SourceEvent", event.payload.navident);
        }

        Personid personid = Personid
                .builder()
                .personid(event.payload.personid)
                .type(event.payload.personidtype)
                .build();

        Periode periode = Periode
                .builder()
                .fraOgMed(event.dato)
                .build();

        Navident ident = Navident
                .builder()
                .navident(event.payload.navident)
                .gjeldendePersonid(personid.getPersonid())
                .identType(event.payload.identType)
                .allePersonider(Arrays.asList(personid))
                .aktivePerioder(Arrays.asList(periode))
                .build();

        navidenter.put(event.payload.navident, ident);

        log.debug(ident.maskedToString());
    }

    private void handleDeactivateEvent(SourceEvent event) {
        String navident = event.payload.navident;
        if (!navidenter.containsKey(navident)) {
            throw new IllegalSourceEventException("Finner ingen NavIdent %s. Kan ikke behandle DEAKTIVER SourceEvent", navident);
        }
        if (!navidenter.get(navident).isActive(event.dato)) {
            throw new IllegalSourceEventException("NavIdent %s er ikke aktiv for event dato %s. Kan ikke behandle DEAKTIVER SourceEvent", navident, event.dato);
        }
        if (navidenter.get(navident).senestePeriodeDato().isAfter(event.dato)) {
            throw new IllegalSourceEventException("NavIdent %s har allerede periodedato(er) etter event dato %s. Kan ikke behandle DEAKTIVER SourceEvent", navident, event.dato);
        }

        Navident ident = navidenter.get(navident);
        List<Periode> aktivePerioder = ident.getAktivePerioder().stream().filter(p -> p.getTilOgMed() == null).collect(Collectors.toList());
        if (aktivePerioder.size() < 1) {
            throw new IllegalSourceEventException("NavIdent %s har ingen aktive perioder å avslutte. Kan ikke behandle DEAKTIVER SourceEvent", navident, event.dato);
        }
        if (aktivePerioder.size() > 1) {
            log.warn("NavIdent %s har flere åpne perioder for event dato %s. Lukker alle på angitt dato".formatted(navident, event.dato));
        }

        Navident nyIdent = ident.clone();
        nyIdent.setAktivePerioder(
            ident.getAktivePerioder().stream()
                    .map( p -> p.toBuilder()
                            .tilOgMed(p.getTilOgMed() != null ? p.getTilOgMed() : event.dato)
                            .build()
                    )
                    .collect(Collectors.toList())
        );

        navidenter.put(navident, nyIdent);
        log.debug(nyIdent.maskedToString());
    }

    private void handleReactivateEvent(SourceEvent event) {
        String navident = event.payload.navident;
        if (!navidenter.containsKey(navident)) {
            throw new IllegalSourceEventException("Finner ingen NavIdent %s. Kan ikke behandle REAKTIVER SourceEvent", navident);
        }
        if (navidenter.get(navident).isActive(event.dato)) {
            throw new IllegalSourceEventException("NavIdent %s er allerede aktiv for event dato %s. Kan ikke behandle REAKTIVER SourceEvent", navident, event.dato);
        }
        if (navidenter.get(navident).senestePeriodeDato().isAfter(event.dato)) {
            throw new IllegalSourceEventException("NavIdent %s har allerede periodedato(er) etter event dato %s. Kan ikke behandle REAKTIVER SourceEvent", navident, event.dato);
        }
        if (navidenter.get(navident).getAktivePerioder().stream().anyMatch(p -> p.getTilOgMed() == null)) {
            throw new IllegalSourceEventException("NavIdent %s har allerede aktiv periode. Kan ikke behandle REAKTIVER SourceEvent", navident);
        }

        Navident ident = navidenter.get(navident);
        Navident nyIdent = ident.clone();
        nyIdent.addPeriode(Periode.builder().fraOgMed(event.dato).build());

        navidenter.put(navident, nyIdent);
        log.debug(nyIdent.maskedToString());
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
