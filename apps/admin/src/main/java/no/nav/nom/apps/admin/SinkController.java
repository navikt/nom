package no.nav.nom.apps.admin;

import no.nav.nom.apps.admin.dto.EventSearchParams;
import no.nav.nom.apps.admin.events.orgenhet.OrgenhetEvtsrcRepository;
import no.nav.nom.apps.admin.events.ressurs.RessursEvtsrcRepository;
import no.nav.nom.apps.admin.events.tilgang.TilgangEvtsrcRepository;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

import static no.nav.nom.apps.admin.dto.EventSearchParams.eventFilter;
import static no.nav.nom.apps.admin.utils.safeSubList;

@RestController
@RequestMapping("/sinks")
@PreAuthorize("hasAuthority('ADMIN')")
public class SinkController {

    private final RessursEvtsrcRepository ressursEvtsrcRepository;
    private final OrgenhetEvtsrcRepository orgenhetEvtsrcRepository;
    private final TilgangEvtsrcRepository tilgangEvtsrcRepository;

    public SinkController(
        RessursEvtsrcRepository ressursEvtsrcRepository, OrgenhetEvtsrcRepository orgenhetEvtsrcRepository,
        TilgangEvtsrcRepository tilgangEvtsrcRepository
    ) {
        this.ressursEvtsrcRepository = ressursEvtsrcRepository;
        this.orgenhetEvtsrcRepository = orgenhetEvtsrcRepository;
        this.tilgangEvtsrcRepository = tilgangEvtsrcRepository;
    }


    @GetMapping(value = "/ressursEvents")
    public List<RessursEvent> getRessursEvents(@Valid EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.getFromOffset();
        List<RessursEvent> events = ressursEvtsrcRepository.getRessursEvtsrc().stream().filter(eventFilter(eventSearchParams)).toList();

        return safeSubList(events.reversed(), fromOffset, fromOffset + 100);
    }

    @GetMapping(value = "/orgenhetEvents")
    public List<OrgEnhetEvent> getOrgEnhetEvents(@Valid EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.getFromOffset();
        List<OrgEnhetEvent> events = orgenhetEvtsrcRepository.getOrgenhetEvtsrc().stream().filter(eventFilter(eventSearchParams)).toList();

        return safeSubList(events.reversed(), fromOffset, fromOffset + 100);
    }

    @GetMapping(value = "/tilgangEvents")
    public List<AuthorizationEvent> getTilgangEvents(@Valid EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.getFromOffset();
        List<AuthorizationEvent> events = tilgangEvtsrcRepository.getTilgangEvtsrc().stream().filter(eventFilter(eventSearchParams)).toList();

        return safeSubList(events.reversed(), fromOffset, fromOffset + 100);
    }


}
