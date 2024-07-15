package no.nav.nom.apps.admin.dto;

import jakarta.validation.constraints.Min;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.nav.nom.apps.admin.dto.hendelse.HendelseDTO;
import no.nav.nom.apps.admin.dto.orgenhet.OrgEnhetStateDTO;
import no.nav.nom.apps.admin.dto.ressurs.RessursStateKafkaDTO;
import no.nav.nom.contract.nom.eventsource.Event;

import java.util.function.Predicate;

@Getter
@NoArgsConstructor
@Setter
public final class EventSearchParams {
    @Min(0) private int fromOffset = 0;
    private String filterText = "";

    public static <T> Predicate<T> eventFilter(EventSearchParams eventSearchParams) {
        return e -> e.toString().contains(eventSearchParams.filterText);
    }
}
