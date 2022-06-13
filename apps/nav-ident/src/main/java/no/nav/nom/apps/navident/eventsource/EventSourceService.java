package no.nav.nom.apps.navident.eventsource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.NavIdent;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.domain.Periode;
import no.nav.nom.apps.navident.domain.Personid;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

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

        // Update domain model with sourced event
        switch (event.event) {
            case OPPRETT -> createEvent(event);
            case DEAKTIVER -> deactivateEvent(event);
            case REAKTIVER -> reactivateEvent(event);
            case SLETT -> deleteEvent(event);
            default -> log.error("Unknown event type {}", event.event);
        }
    }


    private void createEvent(SourceEvent event) {
        if (navidenter.containsKey(event.payload.navident)) {
            log.error("Navident {} finnes ifra før. Kan ikke behandle OPPRETT SourceEvent", event.payload.navident);
            return;
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
                .personid(personid)
                .identType(event.payload.identType)
                .periode(periode)
                .build();

        navidenter.put(event.payload.navident, ident);

        log.debug(ident.maskedToString());
    }

    private void deactivateEvent(SourceEvent event) {
        String navident = event.payload.navident;
        if (!navidenter.containsKey(navident)) {
            log.error("Finner ingen NavIdent {}. Kan ikke behandle DEAKTIVER SourceEvent", navident);
            return;
        }
        if (!navidenter.get(navident).isActive(event.dato)) {
            log.error("NavIdent {} er ikke aktiv for dato {}. Kan ikke behandle DEAKTIVER SourceEvent", navident, event.dato);
            return;
        }

        Navident ident = navidenter.get(navident);
        List<Periode> aktivePrioder = ident.getAktivePerioder().stream().filter(p -> p.getTilOgMed() == null).collect(Collectors.toList());
        if (aktivePrioder.size() < 1) {
            log.error("NavIdent {} har allerede avsluttet perode for dato {}. Kan ikke behandle DEAKTIVER SourceEvent", navident, event.dato);
            return;
        }
        if (aktivePrioder.size() > 1) {
            log.warn("NavIdent {} har flere åpne perode for dato {}. Lukker alle på angitt dato", navident, event.dato);
        }
        aktivePrioder.forEach(periode -> periode.setTilOgMed(event.dato));

        log.debug(ident.maskedToString());
    }

    private void reactivateEvent(SourceEvent event) {
        log.error("Reactivate not implemented yet");
    }

    private void deleteEvent(SourceEvent event) {
        log.error("Delete not implemented yet");
    }


}
