package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreNomNivaa;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.NomNivaaEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Component
public class OrgEnhetNomNivaaHandler {

    private final Logger logger = LoggerFactory.getLogger(OrgEnhetNomNivaaHandler.class);

    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgEnhetCommandService orgEnhetCommandService;
    private final OrgEnhetOverordnetResolver orgEnhetOverordnetResolver;
    private final OrgEnhetUnderordnetResolver orgEnhetUnderordnetResolver;

    private final List<String> linjeenheteneOrgEnhetIds;


    public OrgEnhetNomNivaaHandler(
        OrgEnhetStateRepository orgEnhetStateRepository,
        OrgEnhetCommandService orgEnhetCommandService,
        OrgEnhetOverordnetResolver orgEnhetOverordnetResolver,
        OrgEnhetUnderordnetResolver orgEnhetUnderordnetResolver,
        AppProperties appProperties
    ) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.orgEnhetOverordnetResolver = orgEnhetOverordnetResolver;
        this.orgEnhetUnderordnetResolver = orgEnhetUnderordnetResolver;
        this.linjeenheteneOrgEnhetIds = appProperties.orgenhet().linjeenhetene();
    }

    public void mapOrgEnhetTypeToNewNomNivaaAndUpdateChildren(String  fid, OrgEnhetsType newOrgEnhetsType) {
        orgEnhetStateRepository.findByFid(fid).ifPresent(orgEnhetState -> {
                mapOrgEnhetTypeToNewNomNivaaAndUpdate(orgEnhetState, newOrgEnhetsType);
                mapOrgEnhetTypeToNewNomNivaaForChildren(orgEnhetState);
            }

        );
    }

    public void mapOrgEnhetTypeToNewNomNivaaForChildren(OrgEnhetState orgEnhetState) {
        var children = orgEnhetUnderordnetResolver.findUnderordnedeOrgEnheter(orgEnhetState);
        children.forEach(childOrgEnhetState -> {
            if (isNull(childOrgEnhetState.orgEnhetsType())) {
                logger.error("OrgEnhetState {} har ingen orgEnhetsType, kan ikke utlede nomNivaa", orgEnhetState.id());
            }
            mapOrgEnhetTypeToNewNomNivaaAndUpdate(childOrgEnhetState, childOrgEnhetState.orgEnhetsType());
            mapOrgEnhetTypeToNewNomNivaaForChildren(childOrgEnhetState);
        });
    }

    public void ensureArbeidsomraadePresentUnderDriftsenhet(String fid, NomNivaa endretNomNivaa) {
        if (nonNull(endretNomNivaa) && endretNomNivaa.equals(NomNivaa.DRIFTSENHET)) {
            orgEnhetStateRepository.findByFid(fid).ifPresent(
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
                leafWithPathEntry.getValue().stream().noneMatch(orgEnhetInPath -> Objects.equals(orgEnhetInPath.nomNivaa(), NomNivaa.ARBEIDSOMRAADE))
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
                        secondBottomOrgenhet, NomNivaa.ARBEIDSOMRAADE, "MISSING_ARBEIDSOMRAADE_ADDED"
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
    public void mapOrgEnhetTypeToNewNomNivaaAndUpdate(OrgEnhetState orgEnhetState, OrgEnhetsType newOrgEnhetsType) {
        if (linjeenheteneOrgEnhetIds.contains(orgEnhetState.id())) {
            updateNomNivaaIfNewValue(
                orgEnhetState,
                NomNivaa.LINJEENHET,
                "LINJEENHET_ID_MATCH"
            );

        } else if (orgEnhetIsTopmostWithinOrgEnhetType(orgEnhetState, newOrgEnhetsType)) {
            updateNomNivaaIfNewValue(
                orgEnhetState,
                NomNivaaMapper.mapNivaaForTopmostWithinOrgEnhetType(newOrgEnhetsType),
                "TOP"
            );

        } else if (orgEnhetIsSecondTopmostWithinOrgEnhetType(orgEnhetState, newOrgEnhetsType)) {
            updateNomNivaaIfNewValue(
                orgEnhetState,
                NomNivaaMapper.mapNivaaForSecondTopmostWithinOrgEnhetType(newOrgEnhetsType),
                "SECOND_TOP"
            );

        } else if (orgEnhetIsThirdTopmostWithinOrgEnhetType(orgEnhetState, newOrgEnhetsType)) {
            updateNomNivaaIfNewValue(
                orgEnhetState,
                NomNivaaMapper.mapNivaaForThirdTopmostWithinOrgEnhetType(newOrgEnhetsType),
                "THIRD_TOP"
            );
        } else {
            updateNomNivaaIfNewValue(orgEnhetState, null, "BELOW");
        }

    }

    public void updateNomNivaaIfNewValue(OrgEnhetState orgEnhetState, NomNivaa newNomNivaa, String levelWithinOrgenhetstype) {

        var orgenheterOver = findAllOrgenheterOver(orgEnhetState, new ArrayList<>());
        if (orgenheterOver.isEmpty()) {
            logger.info(
                "Orgenhet {} got no overordnet orgenheter from dvh. Assuming incorrect data from dvh. Skipping new NomNivaaEndretEventV1",
                orgEnhetState.id()
            );

        } else if (!Objects.equals(orgEnhetState.nomNivaa(), newNomNivaa)) {
            orgEnhetCommandService.endreNomNivaa(
                orgEnhetState.fid(),
                new EndreNomNivaa(
                    orgEnhetState.id(),
                    newNomNivaa,
                    new InputMetadata(
                        "nom-api",
                        "nom-api",
                        Map.of(
                            "oldValue", Optional.ofNullable(orgEnhetState.nomNivaa()),
                            "newValue", Optional.ofNullable(newNomNivaa),
                            "levelWithinOrgenhetstype", levelWithinOrgenhetstype,
                            "orgEnhetId", orgEnhetState.id(),
                            "orgenheterOver", orgenheterOver
                                .stream().map(o -> Map.of(
                                    "ordEnhetId", o.id(),
                                    "orgEnhetsType", Optional.ofNullable(o.orgEnhetsType())
                                )).toList(),
                            "orgEnhetsType", Optional.ofNullable(orgEnhetState.orgEnhetsType())
                        ),
                        new Origination("nom-api", "nom-api")
                    )
                )
            );
        }
    }

    public boolean orgEnhetIsTopmostWithinOrgEnhetType(OrgEnhetState orgEnhetState, OrgEnhetsType orgEnhetsType) {
        // Dersom ingen forelder oppover i treet har samme orgenhetstype ansees orgenheten som en toppnode i denne sammenheng
        return findAllOrgenheterOver(orgEnhetState).stream()
            .noneMatch(overordnetOrgEnhet -> nonNull(overordnetOrgEnhet.orgEnhetsType()) && overordnetOrgEnhet.orgEnhetsType().equals(orgEnhetsType));
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

    public boolean orgEnhetIsSecondTopmostWithinOrgEnhetType(OrgEnhetState orgEnhetState, OrgEnhetsType orgEnhetsType) {
        var overordnetOrgEnhet = orgEnhetOverordnetResolver.findOverordnetOrgEnhet(orgEnhetState);

        return overordnetOrgEnhet.filter(enhetState -> nonNull(enhetState.orgEnhetsType())
            && overordnetOrgEnhet.get().orgEnhetsType().equals(orgEnhetsType)
            && orgEnhetIsTopmostWithinOrgEnhetType(overordnetOrgEnhet.get(), orgEnhetsType)).isPresent();

    }

    public boolean orgEnhetIsThirdTopmostWithinOrgEnhetType(OrgEnhetState orgEnhetState, OrgEnhetsType orgEnhetsType) {
        var overordnetOrgEnhet = orgEnhetOverordnetResolver.findOverordnetOrgEnhet(orgEnhetState);
        return overordnetOrgEnhet.filter(enhetState -> nonNull(enhetState.orgEnhetsType())
            && enhetState.orgEnhetsType().equals(orgEnhetsType)
            && orgEnhetIsSecondTopmostWithinOrgEnhetType(enhetState, orgEnhetsType)).isPresent();
    }

}
