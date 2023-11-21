package no.nav.nom.apps.api.ressurs.api.v1.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.ressurs.masterdata.NomMastered;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DvhRessursRestService {
    private final NomMastered nomMastered;
    private final RessursStateRepository ressursStateRepository;

    public DvhRessursRestService(NomMastered nomMastered, RessursStateRepository ressursStateRepository) {
        this.nomMastered = nomMastered;
        this.ressursStateRepository = ressursStateRepository;
    }

    public List<DvhRessursDTO> nomMasteredRessurser() {
        nomMastered.navidenter();
        return ressursStateRepository.getAll().stream()
            .map(this::toDTO)
            .toList();
    }

    DvhRessursDTO toDTO(RessursState ressursState) {
        return new DvhRessursDTO(
            ressursState.navident(),
            ressursState.sektor().name(),
            ressursState.visningsnavn().fornavn(),
            ressursState.visningsnavn().mellomnavn(),
            ressursState.visningsnavn().etternavn(),
            ressursState.startdato(),
            ressursState.sluttdato()
        );
    }
}
