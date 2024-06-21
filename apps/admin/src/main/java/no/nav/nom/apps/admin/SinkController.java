package no.nav.nom.apps.admin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.nav.nom.apps.admin.events.orgenhet.OrgenhetEvtsrcRepository;
import no.nav.nom.apps.admin.events.ressurs.RessursEvtsrcRepository;
import no.nav.nom.apps.admin.events.tilgang.TilgangEvtsrcRepository;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@RestController
@RequestMapping("/sinks")
@PreAuthorize("hasAuthority('ADMIN')")
public class SinkController {

    private final RessursEvtsrcRepository ressursEvtsrcRepository;
    private final OrgenhetEvtsrcRepository orgenhetEvtsrcRepository;
    private final TilgangEvtsrcRepository tilgangEvtsrcRepository;

    public SinkController(RessursEvtsrcRepository ressursEvtsrcRepository, OrgenhetEvtsrcRepository orgenhetEvtsrcRepository,
                          TilgangEvtsrcRepository tilgangEvtsrcRepository
    ) {
        this.ressursEvtsrcRepository = ressursEvtsrcRepository;
        this.orgenhetEvtsrcRepository = orgenhetEvtsrcRepository;
        this.tilgangEvtsrcRepository = tilgangEvtsrcRepository;
    }


    @GetMapping(value = "/ressursEvents")
    public List<RessursEvent> getRessursEvents(@Valid EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.fromOffset;
        List<RessursEvent> events = ressursEvtsrcRepository.getRessursEvtsrc().stream().filter(eventFilter(eventSearchParams)).toList();

        return safeSubList(events.reversed(), fromOffset, fromOffset+100);
    }

    @GetMapping(value = "/orgenhetEvents")
    public List<OrgEnhetEvent> getOrgEnhetEvents(@Valid EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.fromOffset;
        List<OrgEnhetEvent> events = orgenhetEvtsrcRepository.getOrgenhetEvtsrc().stream().filter(eventFilter(eventSearchParams)).toList();

        return safeSubList(events.reversed(), fromOffset, fromOffset+100);
    }

    @GetMapping(value = "/tilgangEvents")
    public List<AuthorizationEvent> getTilgangEvents(@Valid EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.fromOffset;
        List<AuthorizationEvent> events = tilgangEvtsrcRepository.getTilgangEvtsrc().stream().filter(eventFilter(eventSearchParams)).toList();

        return safeSubList(events.reversed(), fromOffset, fromOffset+100);
    }

    private static Predicate<Event> eventFilter(EventSearchParams eventSearchParams) {
        return e -> {
            return e.toString().contains(eventSearchParams.filterText);
        };
    }


    @Getter
    @NoArgsConstructor
    @Setter
    public static final class EventSearchParams {
        @Min(0) private int fromOffset = 0;
        private String filterText = "";
    }

    public static <T> List<T> safeSubList(List<T> list, int fromIndex, int toIndex) {
        int size = list.size();
        if (fromIndex >= size || toIndex <= 0 || fromIndex >= toIndex) {
            return Collections.emptyList();
        }

        fromIndex = Math.max(0, fromIndex);
        toIndex = Math.min(size, toIndex);

        return list.subList(fromIndex, toIndex);
    }
}
