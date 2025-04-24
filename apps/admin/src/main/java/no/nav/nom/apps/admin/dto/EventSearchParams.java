package no.nav.nom.apps.admin.dto;

import jakarta.validation.constraints.Min;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.function.Predicate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public final class EventSearchParams {
    @Min(0) private int fromOffset = 0;
    private String filterText = "";

    public static <T> Predicate<T> eventFilter(EventSearchParams eventSearchParams) {
        return e -> e.toString().contains(eventSearchParams.filterText);
    }
}
