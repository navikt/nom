package no.nav.nom.apps.api.ressurs.api.v1.dvh;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.masterdata.NomMastered;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.nom.apps.api.organisering.OrganiseringConfig.UNLEASH_USE_NEW_ORGANISASJON_SOURCE;

@Service
@Slf4j
public class DvhRessursRestService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final NomMastered nomMastered;
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningService orgtilknytningService;
    private final OrgenhetService orgenhetService;
    private final RemedyEnhetIdResolver remedyEnhetIdResolver;
    private final DvhRepository dvhRepository;

    private final OrganiseringStateRepository organiseringStateRepository;
    private final Unleash unleash;

    public DvhRessursRestService(
        NomMastered nomMastered,
        RessursStateRepository ressursStateRepository,
        OrgtilknytningService orgtilknytningService,
        OrgenhetService orgenhetService,
        RemedyEnhetIdResolver remedyEnhetIdResolver,
        DvhRepository dvhRepository, OrganiseringStateRepository organiseringStateRepository, Unleash unleash
    ) {
        this.nomMastered = nomMastered;
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningService = orgtilknytningService;
        this.orgenhetService = orgenhetService;
        this.remedyEnhetIdResolver = remedyEnhetIdResolver;
        this.dvhRepository = dvhRepository;
        this.organiseringStateRepository = organiseringStateRepository;
        this.unleash = unleash;
    }

    public List<DvhRessursDTO> nomMasteredRessurser() {
        return ressursStateRepository
            .findByNavidentBulk(nomMastered.navidenter()).values().stream()
            .map(this::toDTO)
            .toList();
    }

    public List<DvhOrgenhetDTO> orgenheter() {
        boolean useNewOrganisering = unleash.isEnabled(UNLEASH_USE_NEW_ORGANISASJON_SOURCE);
        logger.debug("Henter orgenheter. Bruke ny organisering: {}", useNewOrganisering);

        return orgenhetService.getAllEnheterMedOrganisering().stream()
            .map(oe -> toDTO(oe, useNewOrganisering))
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
            orgtilknytningState.erDagligOppfolging()
        );
    }

    DvhOrgenhetDTO toDTO(IOrgenhet orgenhet, boolean useNewOrganisering) {
        var remedyEnhetId = remedyEnhetIdResolver.remedyEnhetId(orgenhet);

        String overordnet = useNewOrganisering ? getOverordnetEnhetUsingNewOrganisering(orgenhet) : getOverordnetEnhet(orgenhet);

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

    private String getOverordnetEnhet(IOrgenhet orgenhet) {
        var overordnet = dvhRepository.getOrganiseringer(orgenhet.getAgressoId(), orgenhet.getOrgNiv(), orgenhet.getNavn())
            .stream()
            .filter(o ->
                o.getAgressoIdUnder().equals(orgenhet.getAgressoId())
                    && o.getOrgNivUnder().equals(orgenhet.getOrgNiv())
                    && o.getNavnUnder().equals(orgenhet.getNavn()))
            .findFirst();

        if (overordnet.isPresent()) {
            var o = overordnet.get();
            return orgenhetService.getEnhetByAgressoIdNivaAndName(o.getAgressoIdOver(), o.getOrgNivOver(), o.getNavnOver())
                .map(IOrgenhet::getId)
                .orElse(null);
        }
        return null;
    }

    private String getOverordnetEnhetUsingNewOrganisering(IOrgenhet orgenhet) {
        return organiseringStateRepository.findActiveById(orgenhet.getId())
            .map(OrganiseringState::idOver)
            .orElse(null);
    }
}
