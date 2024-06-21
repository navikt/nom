package no.nav.nom.apps.admin;

import no.nav.nom.apps.admin.dto.OrgenhetStateSearchParams;
import no.nav.nom.apps.admin.dto.RessursSearchParams;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

import static no.nav.nom.apps.admin.dto.OrgenhetStateSearchParams.orgenhetSearchFilter;
import static no.nav.nom.apps.admin.dto.RessursSearchParams.ressursSearchFilter;
import static no.nav.nom.apps.admin.utils.safeSubList;

@RestController
@RequestMapping("/states")
@PreAuthorize("hasAuthority('ADMIN')")
public class StateController {

    private final RessursStateRepository ressursStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;


    public StateController(RessursStateRepository ressursStateRepository, OrgEnhetStateRepository orgEnhetStateRepository) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
    }

    @GetMapping(value = "/ressurs")
    public List<RessursState> getRessurser(@Valid RessursSearchParams ressursSearchParams) {
        int fromOffset = ressursSearchParams. getFromOffset();
        var events = ressursStateRepository.getAll().stream().filter(ressursSearchFilter(ressursSearchParams)).toList();

        return safeSubList(events, fromOffset, fromOffset + 100);
    }

    @GetMapping(value = "/orgenhet")
    public List<OrgEnhetState> getOrgenheter(@Valid OrgenhetStateSearchParams orgenhetStateSearchParams) {
        int fromOffset = orgenhetStateSearchParams. getFromOffset();
        var events = orgEnhetStateRepository.getAll().stream().filter(orgenhetSearchFilter(orgenhetStateSearchParams)).toList();

        return safeSubList(events, fromOffset, fromOffset + 100);
    }
}
