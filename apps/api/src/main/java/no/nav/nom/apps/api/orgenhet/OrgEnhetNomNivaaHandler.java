package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreNomNivaa;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class OrgEnhetNomNivaaHandler {

    private final Logger logger = LoggerFactory.getLogger(OrgEnhetNomNivaaHandler.class);

    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgEnhetCommandService orgEnhetCommandService;
    private final OrgEnhetOverordnetResolver orgEnhetOverordnetResolver;

    private final Set<String> linjeenheteneOrgEnhetIds;


    public OrgEnhetNomNivaaHandler(
        OrgEnhetStateRepository orgEnhetStateRepository,
        OrgEnhetCommandService orgEnhetCommandService,
        OrgEnhetOverordnetResolver orgEnhetOverordnetResolver,
        @Value("#{'${app.orgenhet.linjeenhetene}'.split(',')}")
        Set<String> linjeenheteneOrgEnhetIds
    ) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.orgEnhetOverordnetResolver = orgEnhetOverordnetResolver;
        this.linjeenheteneOrgEnhetIds = linjeenheteneOrgEnhetIds;
    }

    // https://jira.adeo.no/browse/ORG-1711
    public void mapOrgEnhetTypeToNewNomNivaaAndUpdate(OrgEnhetTypeEndretEventV1 orgEnhetTypeEndretEventV1) {
        var orgEnhetState = orgEnhetStateRepository.findByFid(orgEnhetTypeEndretEventV1.getFid());
        var newOrgEnhetType = orgEnhetTypeEndretEventV1.getData().orgEnhetsType();

        if (orgEnhetState.isPresent()) {

            if (linjeenheteneOrgEnhetIds.contains(orgEnhetState.get().id())) {
                updateNomNivaaIfNewValue(
                    orgEnhetState.get(),
                    Optional.of(NomNivaa.LINJEENHET),
                    "LINJEENHET_ID_MATCH"
                );

            } else if (orgEnhetIsTopmostWithinOrgEnhetType(orgEnhetState.get(), newOrgEnhetType)) {
                updateNomNivaaIfNewValue(
                    orgEnhetState.get(),
                    newOrgEnhetType.flatMap(NomNivaaMapper::mapNivaaForTopmostWithinOrgEnhetType),
                    "TOP"
                );

            } else if (orgEnhetIsSecondTopmostWithinOrgEnhetType(orgEnhetState.get(), newOrgEnhetType)) {
                updateNomNivaaIfNewValue(
                    orgEnhetState.get(),
                    newOrgEnhetType.flatMap(NomNivaaMapper::mapNivaaForSecondTopmostWithinOrgEnhetType),
                    "SECOND_TOP"
                );

            } else if (orgEnhetIsThirdTopmostWithinOrgEnhetType(orgEnhetState.get(), newOrgEnhetType)){
                updateNomNivaaIfNewValue(
                    orgEnhetState.get(),
                    newOrgEnhetType.flatMap(NomNivaaMapper::mapNivaaForThirdTopmostWithinOrgEnhetType),
                    "THIRD_TOP"
                );
            } else {
                updateNomNivaaIfNewValue(orgEnhetState.get(), Optional.empty(), "BELOW");
            }
        }
    }


    public void updateNomNivaaIfNewValue(OrgEnhetState orgEnhetState, Optional<NomNivaa> newNomNivaa, String levelWithinOrgenhetstype) {

        var orgenheterOver = findAllOrgenheterOver(orgEnhetState, new ArrayList<>());

        if (orgenheterOver.isEmpty()){
            logger.info(
                "Orgenhet {} got no overordnet orgenheter from dvh. Assuming incorrect data from dvh. Skipping new NomNivaaEndretEventV1",
                orgEnhetState.id()
            );

        } else if (!orgEnhetState.nomNivaa().equals(newNomNivaa)) {
                orgEnhetCommandService.endreNomNivaa(
                    orgEnhetState.fid(),
                    new EndreNomNivaa(
                        newNomNivaa,
                        new InputMetadata(
                            "nom-api",
                            "nom-api",
                            Map.of(
                                "oldValue", orgEnhetState.nomNivaa(),
                                "newValue", newNomNivaa,
                                "levelWithinOrgenhetstype", levelWithinOrgenhetstype,
                                "orgEnhetId", orgEnhetState.id(),
                                "orgenheterOver", orgenheterOver
                                    .stream().map(o -> Map.of(
                                        "ordEnhetId", o.id(),
                                        "orgEnhetsType", o.orgEnhetsType()
                                    )).toList(),
                                "orgEnhetsType", orgEnhetState.orgEnhetsType()
                            ),
                            new Origination("nom-api", "nom-api"))
                    )
                );
        }
    }

    public boolean orgEnhetIsTopmostWithinOrgEnhetType(OrgEnhetState orgEnhetState, Optional<OrgEnhetsType> orgEnhetsType) {
        // Dersom ingen forelder oppover i treet har samme orgenhetstype ansees orgenheten som en toppnode i denne sammenheng
        return findAllOrgenheterOver(orgEnhetState).stream()
            .noneMatch(overordnetOrgEnhet -> overordnetOrgEnhet.orgEnhetsType().equals(orgEnhetsType));
    }


    private List<OrgEnhetState> findAllOrgenheterOver(OrgEnhetState orgEnhetState) {
        return findAllOrgenheterOver(orgEnhetState, new ArrayList<>());
    }

    private List<OrgEnhetState> findAllOrgenheterOver(OrgEnhetState orgEnhetState, ArrayList<OrgEnhetState> orgenheterSoFar) {
        return orgEnhetOverordnetResolver.findOverordnetOrgEnhet(orgEnhetState)
            .map(overordnetOrgenhet -> {
                orgenheterSoFar.add(overordnetOrgenhet);
                return findAllOrgenheterOver(overordnetOrgenhet, orgenheterSoFar);
            })
            .orElse(orgenheterSoFar);

    }

    public boolean orgEnhetIsSecondTopmostWithinOrgEnhetType(OrgEnhetState orgEnhetState, Optional<OrgEnhetsType> orgEnhetsType) {
        var overordnetOrgEnhet = orgEnhetOverordnetResolver.findOverordnetOrgEnhet(orgEnhetState);

        return overordnetOrgEnhet.isPresent()
            && overordnetOrgEnhet.get().orgEnhetsType().equals(orgEnhetsType)
            && orgEnhetIsTopmostWithinOrgEnhetType(overordnetOrgEnhet.get(), orgEnhetsType);

    }

    public boolean orgEnhetIsThirdTopmostWithinOrgEnhetType(OrgEnhetState orgEnhetState, Optional<OrgEnhetsType> orgEnhetsType) {
        var overordnetOrgEnhet = orgEnhetOverordnetResolver.findOverordnetOrgEnhet(orgEnhetState);

        return overordnetOrgEnhet.isPresent()
            && overordnetOrgEnhet.get().orgEnhetsType().equals(orgEnhetsType)
            && orgEnhetIsSecondTopmostWithinOrgEnhetType(overordnetOrgEnhet.get(), orgEnhetsType);
    }

}
