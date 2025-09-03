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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                        var orgEnhetsTypeNotEqualToExistingValue = leafOrgEnhet.orgEnhetsType() != nomOrgenhetType;

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
        orgEnhetOverordnetResolver.findOverordnetOrgEnhet(
            orgEnhetStateRepository.findByFid(orgEnhetTypeEndretEventV1.getFid()).orElse(null)
        ).ifPresent(overordnetOrgEnhet -> {
            if (nonNull(overordnetOrgEnhet.orgEnhetsType())) {
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

            Set<OrgEnhetsType> orgEnhetsTyperTilBarna = orgEnhetStateRepository.getAll().stream().filter(this::isOrgEnhetActive)
                .filter(orgEnhetState -> {
                    Optional<OrgEnhetState> overordnetOrgEnhetForIt = orgEnhetOverordnetResolver.findOverordnetOrgEnhet(orgEnhetState);
                    return overordnetOrgEnhetForIt.filter(enhetState -> overordnetOrgEnhet.id().equals(enhetState.id())).isPresent();
                })
                .flatMap(orgEnhetBarn -> {
                    OrgEnhetsType orgEnhetsTypeFromEventSource = orgEnhetTypeEndretEventV1.getData().orgEnhetsType();
                    // Ekskluder type fra staten til fid for dette eventet da staten ikke nødvendigvis har blitt oppdatert ennå
                    if (orgEnhetBarn.fid().equals(orgEnhetTypeEndretEventV1.getFid())) {
                        return Stream.of(orgEnhetsTypeFromEventSource);
                    } else {
                        return Stream.of(orgEnhetBarn.orgEnhetsType(), orgEnhetsTypeFromEventSource);
                    }
                })
                .collect(Collectors.toSet());

            if (orgEnhetsTyperTilBarna.size() == 1) {
                var newOrgEnhetsType = orgEnhetsTyperTilBarna.stream().toList().getFirst();
                if (!Objects.equals(overordnetOrgEnhet.orgEnhetsType(), newOrgEnhetsType)) {
                    var endreType = new EndreOrgEnhetsType(
                        newOrgEnhetsType,
                        new InputMetadata("kafka: org.nom.orgenhet-eventsource", "orgEnhetsTypeChanged", null, new Origination("nom-api", "nom-api"))
                    );
                    orgEnhetCommandService.endreOrgEnhetsType(overordnetOrgEnhet.fid(), endreType);
                    logger.debug("Changed orgenhet {} because all its children are of orgEnhetsType {}", overordnetOrgEnhet.fid(), newOrgEnhetsType);
                }
            } else {
                // Log id til overordnet orgenhet sammen med hvilke orgtyper barna dens har. Så lenge alle barna ikke har samme så må det bestemmes manuelt.
                logger.debug(
                    "Did not change orgenhet {} because children are of differents orgEnhetsTypes {}",
                    overordnetOrgEnhet.fid(),
                    orgEnhetsTyperTilBarna
                );
            }
        });
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
