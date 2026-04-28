package no.nav.nom.apps.api.ressurs.api.v1.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.propertyresolvers.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.contract.nom.IdentType;
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

import static java.util.Objects.nonNull;

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

    public List<DvhOrgenhetDTO> orgenheter() {
        logger.debug("Henter orgenheter. Bruke ny organisering");

        return orgenhetService.getAktiveEnheterIOrganisasjonen().stream()
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
            ressursState.personident(),
            ressursState.sektor().name(),
            ressursState.identType() != null ? ressursState.identType().name() : IdentType.STANDARD.name(),
            ressursState.visningsnavn().fornavn(),
            ressursState.visningsnavn().mellomnavn().orElse(null),
            ressursState.visningsnavn().etternavn(),
            ressursState.startdato(),
            ressursState.sluttdato(),
            orgtilknytninger,
            ressursState.tilknytningtype() !=null? ressursState.tilknytningtype().name(): null,
            ressursState.primaryFysiskBrukersted(),
            ressursState.jobbtelefon(),
            ressursState.primaryTelefonnummer()
        );
    }

    private DvhOrgtilknytningDTO toDvhOrgtilknytningDTO(OrgtilknytningState orgtilknytningState) {
        return new DvhOrgtilknytningDTO(
            orgtilknytningState.orgEnhetId(),
            orgtilknytningState.gyldigFom(),
            orgtilknytningState.gyldigTom(),
            orgtilknytningState.sektor() != null ? orgtilknytningState.sektor().name() : null,
            orgtilknytningState.erDagligOppfolging(),
            orgtilknytningState.orgtilknytningType() != null ? orgtilknytningState.orgtilknytningType().name() : null,
            orgtilknytningState.fid()
        );
    }

    DvhOrgenhetDTO toDTO(IOrgenhet orgenhet) {
        var remedyEnhetId = remedyEnhetIdResolver.remedyEnhetId(orgenhet);

        String overordnet = getOverordnetEnhetUsingNewOrganisering(orgenhet);

        return new DvhOrgenhetDTO(
            orgenhet.getId(),
            orgenhet.getNavn(),
            nonNull(orgenhet.getOrgEnhetsType()) ? orgenhet.getOrgEnhetsType().name() : null,
            nonNull(orgenhet.getNomNivaa()) ? orgenhet.getNomNivaa().name() : null,
            overordnet,
            orgenhet.getGyldigFom(),
            orgenhet.getGyldigTom(),
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
