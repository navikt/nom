package no.nav.nom.apps.api.ressurs.api.v1.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.ressurs.masterdata.NomMastered;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DvhRessursRestService {
    private final NomMastered nomMastered;
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningService orgtilknytningService;

    public DvhRessursRestService(NomMastered nomMastered, RessursStateRepository ressursStateRepository, OrgtilknytningService orgtilknytningService) {
        this.nomMastered = nomMastered;
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningService = orgtilknytningService;
    }

    public List<DvhRessursDTO> nomMasteredRessurser() {
        return ressursStateRepository
            .findByNavidentBulk(nomMastered.navidenter()).values().stream()
            .map(this::toDTO)
            .toList();
    }

    DvhRessursDTO toDTO(RessursState ressursState) {
        List<DvhOrgtilknytningDTO> orgtilknytninger = orgtilknytningService.getByNavIdent(ressursState.navident())
            .stream()
            .map(this::toDvhOrgtilknytningDTO)
            .toList();
        return new DvhRessursDTO(
            ressursState.navident(),
            ressursState.sektor().name(),
            ressursState.visningsnavn().fornavn(),
            ressursState.visningsnavn().mellomnavn(),
            ressursState.visningsnavn().etternavn(),
            ressursState.startdato(),
            ressursState.sluttdato(),
            orgtilknytninger
        );
    }

    private DvhOrgtilknytningDTO toDvhOrgtilknytningDTO(OrgtilknytningState orgtilknytningState) {
        return new DvhOrgtilknytningDTO(
            orgtilknytningState.orgEnhetId(),
            orgtilknytningState.gyldigFom(),
            orgtilknytningState.gyldigTom(),
            orgtilknytningState.erDagligOppfolging()
        );
    }
}
