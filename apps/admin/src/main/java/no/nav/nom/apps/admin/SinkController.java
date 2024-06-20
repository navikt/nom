package no.nav.nom.apps.admin;

import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@RestController
@RequestMapping("/sinks")
@PreAuthorize("hasAuthority('ADMIN')")
public class SinkController {

    private final RessursEvtsrcRepository ressursEvtsrcRepository;
    private final OrgenhetEvtsrcRepository orgenhetEvtsrcRepository;

    public SinkController(RessursEvtsrcRepository ressursEvtsrcRepository, OrgenhetEvtsrcRepository orgenhetEvtsrcRepository) {
        this.ressursEvtsrcRepository = ressursEvtsrcRepository;
        this.orgenhetEvtsrcRepository = orgenhetEvtsrcRepository;
    }


    @GetMapping(value = "/ressursEvents")
    public List<RessursEvent> getRessursEvents(@ModelAttribute EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.fromOffset;
        List<RessursEvent> events = ressursEvtsrcRepository.getRessursEvtsrc().stream().filter(eventFilter(eventSearchParams)).toList();

        return safeSubList(events.reversed(), fromOffset, fromOffset+100);
    }

    @GetMapping(value = "/orgenhetEvents")
    public List<OrgEnhetEvent> getOrgEnhetEvents(@ModelAttribute EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.fromOffset;
        List<OrgEnhetEvent> events = orgenhetEvtsrcRepository.getOrgenhetEvtsrc().stream().filter(eventFilter(eventSearchParams)).toList();

        return safeSubList(events.reversed(), fromOffset, fromOffset+100);
    }

    private static Predicate<Event> eventFilter(EventSearchParams eventSearchParams) {
        return e -> {
            return e.toString().contains(eventSearchParams.filterText);
        };
    }

    public record EventSearchParams(
        @RequestParam(defaultValue = "0") int fromOffset,
        @RequestParam(defaultValue = "") String filterText
    ) {}

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
