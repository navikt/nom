package no.nav.nom.apps.navident.eventsource;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
public class SourceEvent {
    EventType event;
    LocalDateTime eventTS;
    LocalDate dato;
    String bestiller;
    EventPayload payload;
}
