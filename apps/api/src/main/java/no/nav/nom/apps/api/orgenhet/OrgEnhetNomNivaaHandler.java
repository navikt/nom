package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreNomNivaa;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.NomNivaaEndretEventV1;
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
import java.util.stream.Collectors;

@Component
public class OrgEnhetNomNivaaHandler {

    private final Logger logger = LoggerFactory.getLogger(OrgEnhetNomNivaaHandler.class);

    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgEnhetCommandService orgEnhetCommandService;
    private final OrgEnhetOverordnetResolver orgEnhetOverordnetResolver;
    private final OrgEnhetUnderordnetResolver orgEnhetUnderordnetResolver;

    private final Set<String> linjeenheteneOrgEnhetIds;


    public OrgEnhetNomNivaaHandler(
        OrgEnhetStateRepository orgEnhetStateRepository,
        OrgEnhetCommandService orgEnhetCommandService,
        OrgEnhetOverordnetResolver orgEnhetOverordnetResolver,
        OrgEnhetUnderordnetResolver orgEnhetUnderordnetResolver, @Value("#{'${app.orgenhet.linjeenhetene}'.split(',')}")
        Set<String> linjeenheteneOrgEnhetIds
    ) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.orgEnhetOverordnetResolver = orgEnhetOverordnetResolver;
        this.orgEnhetUnderordnetResolver = orgEnhetUnderordnetResolver;
        this.linjeenheteneOrgEnhetIds = linjeenheteneOrgEnhetIds;
    }

    public void mapOrgEnhetTypeToNewNomNivaaAndUpdateChildren(OrgEnhetTypeEndretEventV1 orgEnhetTypeEndretEventV1) {

        orgEnhetStateRepository.findByFid(orgEnhetTypeEndretEventV1.getFid()).ifPresent(orgEnhetState -> {
                mapOrgEnhetTypeToNewNomNivaaAndUpdate(orgEnhetState, orgEnhetTypeEndretEventV1.getData().orgEnhetsType());
                mapOrgEnhetTypeToNewNomNivaaForChildren(orgEnhetState);
            }

        );
    }

    public void mapOrgEnhetTypeToNewNomNivaaForChildren(OrgEnhetState orgEnhetState) {
        var children = orgEnhetUnderordnetResolver.findUnderordnedeOrgEnheter(orgEnhetState);
        children.forEach(childOrgEnhetState -> {
            mapOrgEnhetTypeToNewNomNivaaAndUpdate(childOrgEnhetState, childOrgEnhetState.orgEnhetsType());
            mapOrgEnhetTypeToNewNomNivaaForChildren(childOrgEnhetState);
        });
    }

    public void ensureArbeidsomraadePresentUnderDriftsenhet(NomNivaaEndretEventV1 nomNivaaEndretEventV1) {
        var nomNivaa = nomNivaaEndretEventV1.getNomNivaaEndretEventData().nomNivaa();

        if (nomNivaa.equals(Optional.of(NomNivaa.DRIFTSENHET))) {
            orgEnhetStateRepository.findByFid(nomNivaaEndretEventV1.getFid()).ifPresent(
                orgEnhetState -> {
                    addArbeidsomraadeInAllSubPathsIfMissing(orgEnhetState);
                    logger.debug("Ensured that ARBEIDSOMRAADE is set under DRIFTSENHET {}", orgEnhetState.id());
                }
            );
        }

    }

    public void addArbeidsomraadeInAllSubPathsIfMissing(OrgEnhetState topOrgEnhetState) {

        var secondBottomOrgenheterToUpdate = findAllPathsFromLeafToNode(topOrgEnhetState).stream()
            .filter(leafWithPathEntry ->
                leafWithPathEntry.getValue().stream().noneMatch(orgEnhetInPath -> orgEnhetInPath.nomNivaa().equals(Optional.of(NomNivaa.ARBEIDSOMRAADE)))
            )
            .map(leafWithNoArbeidsomraadeInPath -> orgEnhetOverordnetResolver.findOverordnetOrgEnhet(leafWithNoArbeidsomraadeInPath.getKey()))
            .flatMap(Optional::stream)
            .filter(this::isOrgenhetSecondBottomInAllPaths)
            .collect(Collectors.toSet());

        secondBottomOrgenheterToUpdate.forEach(
            secondBottomOrgenhet -> {

                // Safeguard, this should never be the case, but it is theoretically possible
                if (!secondBottomOrgenhet.id().equals(topOrgEnhetState.id())) {
                    logger.info("Adding missing ARBEIDSOMRAADE {} under DRIFTSENHET {}", secondBottomOrgenhet.id(), topOrgEnhetState.id());

                    updateNomNivaaIfNewValue(
                        secondBottomOrgenhet, Optional.of(NomNivaa.ARBEIDSOMRAADE), "MISSING_ARBEIDSOMRAADE_ADDED"
                    );
                }
            }
        );
    }

    private boolean isOrgenhetSecondBottomInAllPaths(OrgEnhetState orgEnhetState) {
        var children = orgEnhetUnderordnetResolver.findUnderordnedeOrgEnheter(orgEnhetState);

        return children.stream().allMatch(child -> orgEnhetUnderordnetResolver.findUnderordnedeOrgEnheter(child).isEmpty());
    }

    public List<Map.Entry<OrgEnhetState, List<OrgEnhetState>>> findAllPathsFromLeafToNode(OrgEnhetState topOrgEnhetState) {

        return findAllLeavesFromNode(topOrgEnhetState, new ArrayList<>()).stream()
            .map(leaf -> Map.entry(leaf, findPathToTop(leaf, topOrgEnhetState)))
            .toList();

    }

    public List<OrgEnhetState> findPathToTop(OrgEnhetState fromOrgEnhet, OrgEnhetState topOrgEnhet) {
        return findPathToTop(fromOrgEnhet, topOrgEnhet, new ArrayList<>(List.of(fromOrgEnhet)));
    }


    private List<OrgEnhetState> findPathToTop(OrgEnhetState fromOrgEnhet, OrgEnhetState topOrgEnhet, ArrayList<OrgEnhetState> orgenheterSoFar) {
        var overordnetOrgEnhet = orgEnhetOverordnetResolver.findOverordnetOrgEnhet(fromOrgEnhet);

        if (overordnetOrgEnhet.isPresent()) {
            if (overordnetOrgEnhet.get().id().equals(topOrgEnhet.id())) {
                orgenheterSoFar.add(overordnetOrgEnhet.get());

                return orgenheterSoFar;
            }
            orgenheterSoFar.add(overordnetOrgEnhet.get());

            return findAllOrgenheterOver(overordnetOrgEnhet.get(), orgenheterSoFar);
        }

        return orgenheterSoFar;

    }


    public ArrayList<OrgEnhetState> findAllLeavesFromNode(OrgEnhetState topOrgEnhetState, ArrayList<OrgEnhetState> leavesSoFar) {
        var children = orgEnhetUnderordnetResolver.findUnderordnedeOrgEnheter(topOrgEnhetState);

        var isLeavesMap = children.stream().collect(Collectors.partitioningBy(
            child -> orgEnhetUnderordnetResolver.findUnderordnedeOrgEnheter(child).isEmpty()
        ));

        var leaves = isLeavesMap.get(true);
        var nonLeaves = isLeavesMap.get(false);

        leavesSoFar.addAll(leaves);

        if (!nonLeaves.isEmpty()) {
            nonLeaves.forEach(
                nonLeaf -> leavesSoFar.addAll(findAllLeavesFromNode(nonLeaf, new ArrayList<>()))
            );
        }

        return leavesSoFar;

    }

    // https://jira.adeo.no/browse/ORG-1711
    public void mapOrgEnhetTypeToNewNomNivaaAndUpdate(OrgEnhetState orgEnhetState, Optional<OrgEnhetsType> newOrgEnhetsType) {


        if (linjeenheteneOrgEnhetIds.contains(orgEnhetState.id())) {
            updateNomNivaaIfNewValue(
                orgEnhetState,
                Optional.of(NomNivaa.LINJEENHET),
                "LINJEENHET_ID_MATCH"
            );

        } else if (orgEnhetIsTopmostWithinOrgEnhetType(orgEnhetState, newOrgEnhetsType)) {
            updateNomNivaaIfNewValue(
                orgEnhetState,
                newOrgEnhetsType.flatMap(NomNivaaMapper::mapNivaaForTopmostWithinOrgEnhetType),
                "TOP"
            );

        } else if (orgEnhetIsSecondTopmostWithinOrgEnhetType(orgEnhetState, newOrgEnhetsType)) {
            updateNomNivaaIfNewValue(
                orgEnhetState,
                newOrgEnhetsType.flatMap(NomNivaaMapper::mapNivaaForSecondTopmostWithinOrgEnhetType),
                "SECOND_TOP"
            );

        } else if (orgEnhetIsThirdTopmostWithinOrgEnhetType(orgEnhetState, newOrgEnhetsType)) {
            updateNomNivaaIfNewValue(
                orgEnhetState,
                newOrgEnhetsType.flatMap(NomNivaaMapper::mapNivaaForThirdTopmostWithinOrgEnhetType),
                "THIRD_TOP"
            );
        } else {
            updateNomNivaaIfNewValue(orgEnhetState, Optional.empty(), "BELOW");
        }

    }

    public void updateNomNivaaIfNewValue(OrgEnhetState orgEnhetState, Optional<NomNivaa> newNomNivaa, String levelWithinOrgenhetstype) {

        var orgenheterOver = findAllOrgenheterOver(orgEnhetState, new ArrayList<>());

        if (orgenheterOver.isEmpty()) {
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
                        new Origination("nom-api", "nom-api")
                    )
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
