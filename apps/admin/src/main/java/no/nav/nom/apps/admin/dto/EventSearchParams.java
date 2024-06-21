package no.nav.nom.apps.admin.dto;

import jakarta.validation.constraints.Min;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.nav.nom.contract.nom.eventsource.Event;

import java.util.function.Predicate;

@Getter
@NoArgsConstructor
@Setter
public final class EventSearchParams {
    @Min(0) private int fromOffset = 0;
    private String filterText = "";

    public static Predicate<Event> eventFilter(EventSearchParams eventSearchParams) {
        return e -> {
            return e.toString().contains(eventSearchParams.filterText);
        };
    }
}
