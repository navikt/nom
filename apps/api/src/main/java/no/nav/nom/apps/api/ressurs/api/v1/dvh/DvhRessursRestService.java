package no.nav.nom.apps.api.ressurs.api.v1.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DvhRessursRestService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final MasteredRessursStateRepository masteredRessursStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningService orgtilknytningService;
    private final OrgenhetService orgenhetService;
    private final RemedyEnhetIdResolver remedyEnhetIdResolver;

    private final OrganiseringStateRepository organiseringStateRepository;

    public DvhRessursRestService(
        MasteredRessursStateRepository masteredRessursStateRepository,
        RessursStateRepository ressursStateRepository,
        OrgtilknytningService orgtilknytningService,
        OrgenhetService orgenhetService,
        RemedyEnhetIdResolver remedyEnhetIdResolver,
        OrganiseringStateRepository organiseringStateRepository
    ) {
        this.masteredRessursStateRepository = masteredRessursStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningService = orgtilknytningService;
        this.orgenhetService = orgenhetService;
        this.remedyEnhetIdResolver = remedyEnhetIdResolver;
        this.organiseringStateRepository = organiseringStateRepository;
    }

    public List<DvhRessursDTO> nomMasteredRessurser() {
        return ressursStateRepository
            .findByNavidentBulk(masteredRessursStateRepository.navidenter()).values().stream()
            .map(this::toDTO)
            .toList();
    }

    public List<DvhRessursDTO> nomMasteredRessurser(String startsWith) {
        return ressursStateRepository
            .findByNavidentBulk(masteredRessursStateRepository.navidenter()).values().stream()
            .filter(ressursState -> ressursState.navident().startsWith(startsWith))
            .map(this::toDTO)
            .toList();
    }

    public List<DvhRessursDTO> nomMasteredRessurser(Integer mod30Filter) {
        final Integer restrictedModulo = mod30Filter != null ? mod30Filter % 30 : null;
        return ressursStateRepository
            .findByNavidentBulk(masteredRessursStateRepository.navidenter()).values().stream()
            .filter(it -> restrictedModulo == null || (Integer.parseInt(it.navident().substring(1)) + restrictedModulo) % 30 == 0)
            .map(this::toDTO)
            .toList();
    }

    public List<DvhOrgenhetDTO> orgenheter() {
        logger.debug("Henter orgenheter. Bruke ny organisering");

        return orgenhetService.getAllEnheterMedOrganisering().stream()
            .map(oe -> toDTO(oe))
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
            ressursState.visningsnavn().mellomnavn().orElse(null),
            ressursState.visningsnavn().etternavn(),
            ressursState.startdato().orElse(null),
            ressursState.sluttdato().orElse(null),
            orgtilknytninger
        );
    }

    private DvhOrgtilknytningDTO toDvhOrgtilknytningDTO(OrgtilknytningState orgtilknytningState) {
        return new DvhOrgtilknytningDTO(
            orgtilknytningState.orgEnhetId(),
            orgtilknytningState.gyldigFom(),
            orgtilknytningState.gyldigTom().orElse(null),
            orgtilknytningState.sektor() != null ? orgtilknytningState.sektor().name() : null,
            orgtilknytningState.erDagligOppfolging(),
            orgtilknytningState.orgtilknytningType() != null && orgtilknytningState.orgtilknytningType().isPresent() ? orgtilknytningState.orgtilknytningType().get().name() : null,
            orgtilknytningState.fid()
        );
    }

    DvhOrgenhetDTO toDTO(IOrgenhet orgenhet) {
        var remedyEnhetId = remedyEnhetIdResolver.remedyEnhetId(orgenhet);

        String overordnet = getOverordnetEnhetUsingNewOrganisering(orgenhet);

        return new DvhOrgenhetDTO(
            orgenhet.getId(),
            orgenhet.getNavn(),
            orgenhet.getOrgEnhetsType().isPresent() ? orgenhet.getOrgEnhetsType().get().name() : null,
            orgenhet.getNomNivaa().isPresent() ? orgenhet.getNomNivaa().get().name() : null,
            overordnet,
            orgenhet.getGyldigFom(),
            orgenhet.getGyldigTom().orElse(null),
            orgenhet.getAgressoId(),
            orgenhet.getOrgNiv(),
            remedyEnhetId
        );
    }

    private String getOverordnetEnhetUsingNewOrganisering(IOrgenhet orgenhet) {
        return organiseringStateRepository.findActiveById(orgenhet.getId())
            .map(OrganiseringState::idOver)
            .orElse(null);
    }
}
