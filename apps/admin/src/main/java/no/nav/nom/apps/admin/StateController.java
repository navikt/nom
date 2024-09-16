package no.nav.nom.apps.admin;

import no.nav.nom.apps.admin.dto.OrganiseringStateSearchParams;
import no.nav.nom.apps.admin.dto.OrgenhetStateSearchParams;
import no.nav.nom.apps.admin.dto.OrgtilknytningStateSearchParams;
import no.nav.nom.apps.admin.dto.RessursSearchParams;
import no.nav.nom.apps.admin.dto.ressurs.MasteredRessursDTO;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

import static no.nav.nom.apps.admin.dto.OrganiseringStateSearchParams.organiseringStateSearchFilter;
import static no.nav.nom.apps.admin.dto.OrgenhetStateSearchParams.orgenhetSearchFilter;
import static no.nav.nom.apps.admin.dto.OrgtilknytningStateSearchParams.orgtilknytningStateSearchFilter;
import static no.nav.nom.apps.admin.dto.RessursSearchParams.ressursSearchFilter;
import static no.nav.nom.apps.admin.utils.safeSubList;

@RestController
@RequestMapping("/states")
@PreAuthorize("hasAuthority('ADMIN')")
public class StateController {

    private static final int NUMBER_OF_EVENTS = 500;

    private final RessursStateRepository ressursStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final MasteredRessursStateRepository masteredRessursStateRepository;


    public StateController(
        RessursStateRepository ressursStateRepository, OrgEnhetStateRepository orgEnhetStateRepository,
        OrgtilknytningStateRepository orgtilknytningStateRepository, OrganiseringStateRepository organiseringStateRepository,
        MasteredRessursStateRepository masteredRessursStateRepository
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.organiseringStateRepository = organiseringStateRepository;
        this.masteredRessursStateRepository = masteredRessursStateRepository;
    }

    @GetMapping(value = "/ressurs")
    public List<RessursState> getRessurser(@Valid RessursSearchParams ressursSearchParams) {
        int fromOffset = ressursSearchParams.getFromOffset();
        var events = ressursStateRepository.getAll().stream().filter(ressursSearchFilter(ressursSearchParams)).toList();

        return safeSubList(events, fromOffset, fromOffset + NUMBER_OF_EVENTS);
    }

    @GetMapping(value = "/orgenhet")
    public List<OrgEnhetState> getOrgenheter(@Valid OrgenhetStateSearchParams orgenhetStateSearchParams) {
        int fromOffset = orgenhetStateSearchParams.getFromOffset();
        var events = orgEnhetStateRepository.getAll().stream().filter(orgenhetSearchFilter(orgenhetStateSearchParams)).toList();

        return safeSubList(events, fromOffset, fromOffset + NUMBER_OF_EVENTS);
    }

    @GetMapping(value = "/orgtilknytning")
    public List<OrgtilknytningState> getOrgilknytning(@Valid OrgtilknytningStateSearchParams orgtilknytningStateSearchParams) {
        int fromOffset = orgtilknytningStateSearchParams.getFromOffset();
        var events = orgtilknytningStateRepository
            .getAll().stream().filter(orgtilknytningStateSearchFilter(orgtilknytningStateSearchParams)).toList();

        return safeSubList(events, fromOffset, fromOffset + NUMBER_OF_EVENTS);
    }

    @GetMapping(value = "/organisering")
    public List<OrganiseringState> getOrganisering(@Valid OrganiseringStateSearchParams organiseringStateSearchParams) {
        int fromOffset = organiseringStateSearchParams.getFromOffset();
        var events = organiseringStateRepository
            .getOrganiseringer().stream().filter(organiseringStateSearchFilter(organiseringStateSearchParams)).toList();

        return safeSubList(events, fromOffset, fromOffset + NUMBER_OF_EVENTS);
    }

    @GetMapping(value = "/nom-mastered")
    public List<String> getMasteredNavidentsIds() {
        return masteredRessursStateRepository.navidenter();
    }

    @GetMapping(value = "/nom-mastered-state")
    public List<MasteredRessursDTO> getMasteredNavidents() {
        return masteredRessursStateRepository.getMasteredNavidenter().entrySet().stream()
            .map(masteredEntry -> MasteredRessursDTO.from(masteredEntry.getKey(), masteredEntry.getValue()))
            .toList();
    }
}
