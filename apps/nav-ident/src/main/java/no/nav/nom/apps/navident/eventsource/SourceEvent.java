package no.nav.nom.apps.navident.eventsource;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder(toBuilder = true)
@Data
public class SourceEvent {
    EventType event;
    LocalDateTime eventTS;
    String bestiller;
    EventPayload payload;

    public String maskedToString() {
        SourceEvent masked = this.toBuilder().build(); // Is not a deep copy
        masked.payload = payload.toBuilder().build();
        masked.payload.personid = mask(payload.personid);

        return masked.toString();
    }

    private String mask(String toBeMasked) {
        if (toBeMasked == null || toBeMasked.length() < 7) return toBeMasked;
        return toBeMasked.substring(0, 6) + "***";
    }
}
