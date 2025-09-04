package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Component
public class OrgEnhetTypeHandler {

    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final Logger logger = LoggerFactory.getLogger(OrgEnhetTypeHandler.class);
    private final OrgEnhetCommandService orgEnhetCommandService;
    private final OrgEnhetOverordnetResolver orgEnhetOverordnetResolver;

    public OrgEnhetTypeHandler(
        OrgEnhetStateRepository orgEnhetStateRepository, OrgEnhetCommandService orgEnhetCommandService,
            OrgEnhetOverordnetResolver orgEnhetOverordnetResolver
    ) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.orgEnhetOverordnetResolver = orgEnhetOverordnetResolver;
    }

    public void updateOrgEnhetsTypeForLeafs() {
        var allOrgEnheter = orgEnhetStateRepository.getAll();
        var validOrgEnheter = allOrgEnheter.stream().filter(this::isOrgEnhetActive);

        var leafOrgEnheter = validOrgEnheter.filter(orgEnhetState -> Objects.equals(orgEnhetState.agressoOrgNivaa(), "ORGENHET"))
            .toList();

        leafOrgEnheter.forEach(leafOrgEnhet -> {
                if (nonNull(leafOrgEnhet.agressoOrgenhetType())) {
                    OrgEnhetsType nomOrgenhetType = mapToNomOrgEnhetsType(leafOrgEnhet.agressoOrgenhetType());
                    if (nonNull(nomOrgenhetType)) {
                        var orgEnhetsTypeNotEqualToExistingValue = !Objects.equals(leafOrgEnhet.orgEnhetsType(), nomOrgenhetType);

                        if (orgEnhetsTypeNotEqualToExistingValue) {
                            // Send kafka melding om å oppdatere orgEnhetsType. Antar at type fra agresso er sannheten.
                            final var endreType = new EndreOrgEnhetsType(
                                nomOrgenhetType,
                                new InputMetadata("nom-api BatchJob", "updateOrgEnhetsTypeForLeafs", null, new Origination("nom-api", "nom-api"))
                            );
                            orgEnhetCommandService.endreOrgEnhetsType(leafOrgEnhet.fid(), endreType);
                        }
                    } else {
                        logger.debug("Orgenhet {} har ikke en kjent agressoOrgenhetType ({})", leafOrgEnhet.id(), leafOrgEnhet.agressoOrgenhetType());
                    }
                }
            });

        logger.debug("Satt orgenhet type for {} loevnoder", leafOrgEnheter.size());
    }

    public void orgEnhetsTypeChanged(OrgEnhetTypeEndretEventV1 orgEnhetTypeEndretEventV1) {
        OrgEnhetState endretOrgEnhet = orgEnhetStateRepository.findByFid(orgEnhetTypeEndretEventV1.getFid()).orElse(null);
        var maybeOverOrdnetOrgEnhet = orgEnhetOverordnetResolver.findOverordnetOrgEnhet(endretOrgEnhet);
        if(maybeOverOrdnetOrgEnhet.isEmpty()) {
            return;
        }
        var overordnetOrgEnhet = maybeOverOrdnetOrgEnhet.get();

        if(nonNull(overordnetOrgEnhet.orgEnhetsType())){
            //TODO: Need this check until all orgEnhetTypes DIR are renamed to DIREKTORAT
            var existingOrgEnhetTypeIsDir = overordnetOrgEnhet.orgEnhetsType().equals(OrgEnhetsType.DIR);

            if (!existingOrgEnhetTypeIsDir) {
                logger.debug(
                    "Overordnet enhet {} funnet har allerede type {}",
                    overordnetOrgEnhet.fid(),
                    overordnetOrgEnhet.orgEnhetsType()
                );
                return;
            }
        }

        var andreAktiveUnderenheterStream = orgEnhetStateRepository.getAll().stream()
            .filter(orgEnhetState -> !orgEnhetState.fid().equals(orgEnhetTypeEndretEventV1.getFid()))
            .filter(this::isOrgEnhetActive)
            .filter(orgEnhetState -> overorgenhetMatch(orgEnhetState, overordnetOrgEnhet));


        var endretOrgEnhetsType = orgEnhetTypeEndretEventV1.getData().orgEnhetsType();
        var orgEnhetsTyperTilUnderenheter = andreAktiveUnderenheterStream
            .map(OrgEnhetState::orgEnhetsType)
            .collect(Collectors.<OrgEnhetsType>toSet());
        orgEnhetsTyperTilUnderenheter.add(endretOrgEnhetsType);


        var alleUnderEnhetsTyperErDenSamme = orgEnhetsTyperTilUnderenheter.size() == 1;

        if(!alleUnderEnhetsTyperErDenSamme){
            // Log id til overordnet orgenhet sammen med hvilke orgtyper barna dens har. Så lenge alle barna ikke har samme så må det bestemmes manuelt.
            logger.debug(
                "Did not change orgenhet {} because children are of differents orgEnhetsTypes {}",
                overordnetOrgEnhet.fid(),
                orgEnhetsTyperTilUnderenheter
            );
            return;
        }

        var overOrdnetEnhetTypeSameAsChildren = Objects.equals(overordnetOrgEnhet.orgEnhetsType(), endretOrgEnhetsType);
        if(!overOrdnetEnhetTypeSameAsChildren) {
            var commandToPush = new EndreOrgEnhetsType(
                endretOrgEnhetsType,
                new InputMetadata("kafka: org.nom.orgenhet-eventsource", "orgEnhetsTypeChanged", null, new Origination("nom-api", "nom-api"))
            );
            orgEnhetCommandService.endreOrgEnhetsType(overordnetOrgEnhet.fid(), commandToPush);
            logger.debug("Changed orgenhet {} because all its children are of orgEnhetsType {}", overordnetOrgEnhet.fid(), endretOrgEnhetsType);
        }

    }

    private boolean overorgenhetMatch(OrgEnhetState it, OrgEnhetState overordnetOrgEnhet) {
        Optional<OrgEnhetState> overordnetOrgEnhetForIt = orgEnhetOverordnetResolver.findOverordnetOrgEnhet(it);
            return overordnetOrgEnhetForIt.filter(enhetState -> overordnetOrgEnhet.id().equals(enhetState.id())).isPresent();

    }

    private boolean isOrgEnhetActive(OrgEnhetState orgEnhetState) {
        // If gyldigTom is null it is considered perpetually valid
        return isNull(orgEnhetState.gyldigTom()) || orgEnhetState.gyldigTom().isAfter(LocalDate.now());
    }

    private OrgEnhetsType mapToNomOrgEnhetsType(String agressoOrgenhetType) {
        return switch (agressoOrgenhetType) {
            case "ALS" -> OrgEnhetsType.ARBEIDSLIVSSENTER;
            case "Arbeid og Ytelser" -> OrgEnhetsType.NAV_ARBEID_OG_YTELSER;
            case "ARK" -> OrgEnhetsType.ARBEIDSRAADGIVNING;
            case "Dir" -> OrgEnhetsType.DIREKTORAT;
            case "Doksenter", "Fylke", "Rådgivende overlege" -> OrgEnhetsType.FYLKE;
            case "Familie- og pensjonsytelser" -> OrgEnhetsType.NAV_FAMILIE_OG_PENSJONSYTELSER;
            case "HMS" -> OrgEnhetsType.HJELPEMIDLER_OG_TILRETTELEGGING;
            case "Klageinstans" -> OrgEnhetsType.KLAGEINSTANS;
            case "Kontaktsenter" -> OrgEnhetsType.NAV_KONTAKTSENTER;
            case "Spes" -> OrgEnhetsType.KONTROLL_KONTROLLENHET;
            case "Lokal" -> OrgEnhetsType.NAV_KONTOR;
            case "Tiltak" -> OrgEnhetsType.TILTAK;
            case "Økonomi" -> OrgEnhetsType.NAV_OKONOMITJENESTE;
            default -> null;
        };
    }
}
