package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        var leafOrgEnheter = validOrgEnheter.filter(orgEnhetState -> orgEnhetState.agressoOrgNivaa().map(agressoOrgNivaa -> agressoOrgNivaa.equals("ORGENHET")).orElse(false))
            .toList();

        leafOrgEnheter.forEach(leafOrgEnhet ->
            mapToNomOrgEnhetsType(leafOrgEnhet.agressoOrgenhetType())
                .ifPresentOrElse(
                    nomOrgenhetType -> {
                        var orgEnhetsTypeNotEqualToExistingValue = leafOrgEnhet.orgEnhetsType()
                            .map(existingOrgEnhetsType -> existingOrgEnhetsType != nomOrgenhetType)
                            .orElse(true);

                        if (orgEnhetsTypeNotEqualToExistingValue) {
                            // Send kafka melding om å oppdatere orgEnhetsType. Antar at type fra agresso er sannheten.
                            final var endreType = new EndreOrgEnhetsType(
                                Optional.of(nomOrgenhetType),
                                new InputMetadata("nom-api BatchJob", "updateOrgEnhetsTypeForLeafs", null, new Origination("nom-api", "nom-api"))
                            );
                            orgEnhetCommandService.endreOrgEnhetsType(leafOrgEnhet.fid(), endreType);
                        }
                    },
                    () -> logger.debug("Orgenhet {} har ikke en kjent agressoOrgenhetType ({})", leafOrgEnhet.id(), leafOrgEnhet.agressoOrgenhetType())
                ));

        logger.debug("Satt orgenhet type for {} loevnoder", leafOrgEnheter.size());
    }

    public void orgEnhetsTypeChanged(OrgEnhetTypeEndretEventV1 orgEnhetTypeEndretEventV1) {
        Optional<OrgEnhetState> changedOrgEnhet = orgEnhetStateRepository.findByFid(orgEnhetTypeEndretEventV1.getFid());

        Optional<OrgEnhetState> overordnetOrgEnhet = changedOrgEnhet.flatMap(orgEnhetOverordnetResolver::findOverordnetOrgEnhet);

        if (overordnetOrgEnhet.flatMap(OrgEnhetState::orgEnhetsType).isPresent()) {

            //TODO: Need this check until all orgEnhetTypes DIR are renamed to DIREKTORAT
            var existingOrgEnhetTypeIsDir = overordnetOrgEnhet.get().orgEnhetsType().equals(Optional.of(OrgEnhetsType.DIR));

            if (!existingOrgEnhetTypeIsDir) {
                logger.debug(
                    "Overordnet enhet {} funnet har allerede type {}",
                    overordnetOrgEnhet.get().fid(),
                    overordnetOrgEnhet.get().orgEnhetsType()
                );
                return;
            }
        }

        Optional<Stream<OrgEnhetState>> barnTilOverordnetOrgEnhet = overordnetOrgEnhet.map(orgEnhet ->
            orgEnhetStateRepository.getAll().stream().filter(this::isOrgEnhetActive).filter(orgEnhetState -> {
                var overordnetOrgEnhetForIt = orgEnhetOverordnetResolver.findOverordnetOrgEnhet(orgEnhetState).map(OrgEnhetState::id).orElse(null);
                return orgEnhet.id().equals(overordnetOrgEnhetForIt);
            })
        );

        Optional<Set<Optional<OrgEnhetsType>>> orgenhetsTyperTilBarna = barnTilOverordnetOrgEnhet
            .map(orgEnhetBarn -> {
                    var orgEnhetsTyper = orgEnhetBarn
                        // Ekskluder type fra staten til fid for dette eventet da staten ikke nødvendigvis har blitt oppdatert ennå
                        .filter(orgEnhetState -> !orgEnhetState.fid().equals(orgEnhetTypeEndretEventV1.getFid()))
                        .map(OrgEnhetState::orgEnhetsType).collect(Collectors.toSet());

                    var orgEnhetsTypeFromEventSource = orgEnhetTypeEndretEventV1.getData().orgEnhetsType();

                    orgEnhetsTyper.add(orgEnhetsTypeFromEventSource);

                    return orgEnhetsTyper;
                }
            );


        orgenhetsTyperTilBarna.ifPresent(orgEnhetsTyperTilBarna -> {

            if (orgEnhetsTyperTilBarna.size() == 1) {
                var newOrgEnhetsType = orgEnhetsTyperTilBarna.stream().toList().get(0);
                if (!overordnetOrgEnhet.get().orgEnhetsType().equals(newOrgEnhetsType )){

                    var endreType = new EndreOrgEnhetsType(
                        newOrgEnhetsType,
                        new InputMetadata("kafka: org.nom.orgenhet-eventsource", "orgEnhetsTypeChanged", null, new Origination("nom-api", "nom-api"))
                    );
                    orgEnhetCommandService.endreOrgEnhetsType(overordnetOrgEnhet.get().fid(), endreType);
                    logger.debug("Changed orgenhet {} because all its children are of orgEnhetsType {}", overordnetOrgEnhet.get().fid(), newOrgEnhetsType);
                }
            } else {
                // Log id til overordnet orgenhet sammen med hvilke orgtyper barna dens har. Så lenge alle barna ikke har samme så må det bestemmes manuelt.
                logger.debug(
                    "Did not change orgenhet {} because children are of differents orgEnhetsTypes {}",
                    overordnetOrgEnhet.get().fid(),
                    orgEnhetsTyperTilBarna
                );
            }

        });
    }

    private boolean isOrgEnhetActive(OrgEnhetState orgEnhetState) {
        // If gyldigTom is null it is considered perpetually valid
        return orgEnhetState.gyldigTom().map(gyldigTom -> gyldigTom.isAfter(LocalDate.now())).orElse(true);
    }

    private static Optional<OrgEnhetsType> mapToNomOrgEnhetsType(Optional<String> maybeAgressoOrgenhetType) {
        return maybeAgressoOrgenhetType.map(agressoOrgenhetType -> switch (agressoOrgenhetType) {
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
        });
    }
}
