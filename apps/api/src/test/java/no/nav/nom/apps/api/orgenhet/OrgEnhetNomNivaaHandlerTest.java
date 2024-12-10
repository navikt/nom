package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreNomNivaa;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static no.nav.nom.apps.api.common.TestData.orgEnhetState;
import static no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils.orgEnhetStateBuilderWithDefaultValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrgEnhetNomNivaaHandlerTest extends OrgEnhetTestBase {

    @MockBean
    private OrgEnhetCommandService orgEnhetCommandService;

    @MockBean
    OrgEnhetOverordnetResolver orgEnhetOverordnetResolver;

    @MockBean
    OrgEnhetUnderordnetResolver orgEnhetUnderordnetResolver;

    @Autowired
    private OrgEnhetStateRepository orgEnhetStateRepository;

    @Autowired
    private OrgEnhetNomNivaaHandler orgEnhetNomNivaaHandler;

    @Captor
    ArgumentCaptor<EndreNomNivaa> endreNomNivaaArgumentCaptor;

    @BeforeEach
    public void init(){
        orgEnhetStateRepository.clear();
    }


    @ParameterizedTest(name = "orgEnhetsType {0} and topmost org enhet within orgEnhetType maps to expectedNomNivaa {1}")
    @MethodSource("mapOrgEnhetTypeToNewNomNivaaAndUpdate_should_map_niva_correctly_and_send_event_for_topmost_with_orgenhet_type_input")
    public void mapOrgEnhetTypeToNewNomNivaaAndUpdate_should_map_niva_correctly_and_send_event_for_topmost_with_orgenhet_type(
        OrgEnhetsType orgEnhetsType, NomNivaa exptectedNomNivaa
    ) {

        var orgEnhetState = orgEnhetState().toBuilder()
            .nomNivaa(Optional.of(nomNivaaDifferentFrom(exptectedNomNivaa)))
            .build();

        var overordnetOrgEnhetState = orgEnhetState().toBuilder()
            .orgEnhetsType(Optional.of(orgEnhetsTypeDifferentFrom(orgEnhetsType)))
            .build();

        orgEnhetStateRepository.update(orgEnhetState);

        when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(orgEnhetState))
            .thenReturn(Optional.of(overordnetOrgEnhetState));

        when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(overordnetOrgEnhetState))
            .thenReturn(Optional.empty());

        orgEnhetNomNivaaHandler.mapOrgEnhetTypeToNewNomNivaaAndUpdate(
            orgEnhetState,
            Optional.of(orgEnhetsType)
        );

        verify(orgEnhetCommandService, times(1))
            .endreNomNivaa(any(), endreNomNivaaArgumentCaptor.capture());

        assertThat(endreNomNivaaArgumentCaptor.getValue().nomNivaa()).isEqualTo(Optional.ofNullable(exptectedNomNivaa));
    }

    public static Stream<Arguments> mapOrgEnhetTypeToNewNomNivaaAndUpdate_should_map_niva_correctly_and_send_event_for_topmost_with_orgenhet_type_input() {
        return Stream.of(
            Arguments.of(OrgEnhetsType.DIREKTORAT, NomNivaa.DRIFTSENHET),
            Arguments.of(OrgEnhetsType.DIR, NomNivaa.DRIFTSENHET),
            Arguments.of(OrgEnhetsType.NAV_ARBEID_OG_YTELSER, NomNivaa.DRIFTSENHET),
            Arguments.of(OrgEnhetsType.NAV_FAMILIE_OG_PENSJONSYTELSER, NomNivaa.DRIFTSENHET),
            Arguments.of(OrgEnhetsType.KONTROLL_KONTROLLENHET, NomNivaa.DRIFTSENHET),
            Arguments.of(OrgEnhetsType.HJELPEMIDLER_OG_TILRETTELEGGING, NomNivaa.DRIFTSENHET),
            Arguments.of(OrgEnhetsType.NAV_KONTAKTSENTER, NomNivaa.DRIFTSENHET),
            Arguments.of(OrgEnhetsType.FYLKE, NomNivaa.DRIFTSENHET),
            Arguments.of(OrgEnhetsType.KLAGEINSTANS, NomNivaa.DRIFTSENHET),

            Arguments.of(OrgEnhetsType.NAV_KONTOR, NomNivaa.ARBEIDSOMRAADE),
            Arguments.of(OrgEnhetsType.TILTAK, NomNivaa.ARBEIDSOMRAADE),
            Arguments.of(OrgEnhetsType.ARBEIDSLIVSSENTER, NomNivaa.ARBEIDSOMRAADE),
            Arguments.of(OrgEnhetsType.ARBEIDSRAADGIVNING, NomNivaa.ARBEIDSOMRAADE),

            Arguments.of(OrgEnhetsType.NAV_OKONOMITJENESTE, null)
        );
    }

    @ParameterizedTest(name = "orgEnhetsType {0} and second topmost org enhet within orgEnhetType maps to exptectedNomNivaa {1}")
    @MethodSource("mapOrgEnhetTypeToNewNomNivaaAndUpdate_should_map_niva_correctly_and_send_event_for_second_topmost_with_orgenhet_type_input")
    public void mapOrgEnhetTypeToNewNomNivaaAndUpdate_should_map_niva_correctly_and_send_event_for_second_topmost_with_orgenhet_type(
        OrgEnhetsType orgEnhetsType, NomNivaa exptectedNomNivaa
    ) {

        var orgEnhetState = orgEnhetState().toBuilder()
            .nomNivaa(Optional.of(nomNivaaDifferentFrom(exptectedNomNivaa)))
            .build();

        var overordnetOrgEnhet = orgEnhetState().toBuilder()
                .orgEnhetsType(Optional.of(orgEnhetsType))
                    .build();

        orgEnhetStateRepository.update(orgEnhetState);

        when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(eq(orgEnhetState)))
            .thenReturn(
                Optional.of(overordnetOrgEnhet)
            );

        when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(eq(overordnetOrgEnhet)))
            .thenReturn(
                Optional.of(
                        orgEnhetState().toBuilder()
                        .orgEnhetsType(Optional.of(orgEnhetsTypeDifferentFrom(orgEnhetsType)))
                        .build()
                )
            );

        orgEnhetNomNivaaHandler.mapOrgEnhetTypeToNewNomNivaaAndUpdate(
            orgEnhetState,
            Optional.of(orgEnhetsType)
        );

        verify(orgEnhetCommandService, times(1))
            .endreNomNivaa(any(), endreNomNivaaArgumentCaptor.capture());

        assertThat(endreNomNivaaArgumentCaptor.getValue().nomNivaa()).isEqualTo(Optional.ofNullable(exptectedNomNivaa));
    }

    public static Stream<Arguments> mapOrgEnhetTypeToNewNomNivaaAndUpdate_should_map_niva_correctly_and_send_event_for_second_topmost_with_orgenhet_type_input() {
        return Stream.of(
            Arguments.of(OrgEnhetsType.NAV_OKONOMITJENESTE, NomNivaa.DRIFTSENHET),

            Arguments.of(OrgEnhetsType.DIREKTORAT, NomNivaa.ARBEIDSOMRAADE),
            Arguments.of(OrgEnhetsType.DIR, NomNivaa.ARBEIDSOMRAADE),
            Arguments.of(OrgEnhetsType.NAV_ARBEID_OG_YTELSER, NomNivaa.ARBEIDSOMRAADE),
            Arguments.of(OrgEnhetsType.NAV_FAMILIE_OG_PENSJONSYTELSER, NomNivaa.ARBEIDSOMRAADE),
            Arguments.of(OrgEnhetsType.KONTROLL_KONTROLLENHET, NomNivaa.ARBEIDSOMRAADE),
            Arguments.of(OrgEnhetsType.HJELPEMIDLER_OG_TILRETTELEGGING, NomNivaa.ARBEIDSOMRAADE),
            Arguments.of(OrgEnhetsType.NAV_KONTAKTSENTER, NomNivaa.ARBEIDSOMRAADE),
            Arguments.of(OrgEnhetsType.KLAGEINSTANS, NomNivaa.ARBEIDSOMRAADE),

            Arguments.of(OrgEnhetsType.FYLKE, null),
            Arguments.of(OrgEnhetsType.NAV_KONTOR, null),
            Arguments.of(OrgEnhetsType.TILTAK, null),
            Arguments.of(OrgEnhetsType.ARBEIDSLIVSSENTER, null),
            Arguments.of(OrgEnhetsType.ARBEIDSRAADGIVNING, null)
        );
    }

    @ParameterizedTest(name = "orgEnhetsType {0} and second topmost org enhet within orgEnhetType maps to exptectedNomNivaa {1}")
    @MethodSource("mapOrgEnhetTypeToNewNomNivaaAndUpdate_should_map_niva_correctly_and_send_event_for_third_topmost_with_orgenhet_type_input")
    public void mapOrgEnhetTypeToNewNomNivaaAndUpdate_should_map_niva_correctly_and_send_event_for_third_topmost_with_orgenhet_type(
        OrgEnhetsType orgEnhetsType, NomNivaa exptectedNomNivaa
    ) {

        var targetOrgEnhetState = orgEnhetState().toBuilder()
            .nomNivaa(Optional.of(nomNivaaDifferentFrom(exptectedNomNivaa)))
            .build();

        var overordnetOrgEnhet = orgEnhetState().toBuilder()
            .orgEnhetsType(Optional.of(orgEnhetsType))
            .build();

        var overOverordnetOrgEnhet = orgEnhetState().toBuilder()
            .orgEnhetsType(Optional.of(orgEnhetsType))
            .build();

        orgEnhetStateRepository.update(targetOrgEnhetState);

        when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(eq(targetOrgEnhetState)))
            .thenReturn(
                Optional.of(overordnetOrgEnhet)
            );

        when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(eq(overordnetOrgEnhet)))
            .thenReturn(Optional.of(overOverordnetOrgEnhet));

        when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(eq(overOverordnetOrgEnhet)))
            .thenReturn(
                Optional.of(
                    orgEnhetState().toBuilder()
                        .orgEnhetsType(Optional.empty())
                        .build()
                )
            );

        orgEnhetNomNivaaHandler.mapOrgEnhetTypeToNewNomNivaaAndUpdate(
            targetOrgEnhetState,
            Optional.of(orgEnhetsType)
        );

        verify(orgEnhetCommandService, times(1))
            .endreNomNivaa(any(), endreNomNivaaArgumentCaptor.capture());

        assertThat(endreNomNivaaArgumentCaptor.getValue().nomNivaa()).isEqualTo(Optional.ofNullable(exptectedNomNivaa));
    }

    public static Stream<Arguments> mapOrgEnhetTypeToNewNomNivaaAndUpdate_should_map_niva_correctly_and_send_event_for_third_topmost_with_orgenhet_type_input() {
        return Stream.of(
            Arguments.of(OrgEnhetsType.NAV_OKONOMITJENESTE, NomNivaa.ARBEIDSOMRAADE),
            Arguments.of(OrgEnhetsType.DIREKTORAT, null),
            Arguments.of(OrgEnhetsType.DIR, null),
            Arguments.of(OrgEnhetsType.NAV_ARBEID_OG_YTELSER, null),
            Arguments.of(OrgEnhetsType.NAV_FAMILIE_OG_PENSJONSYTELSER, null),
            Arguments.of(OrgEnhetsType.KONTROLL_KONTROLLENHET, null),
            Arguments.of(OrgEnhetsType.HJELPEMIDLER_OG_TILRETTELEGGING, null),
            Arguments.of(OrgEnhetsType.NAV_KONTAKTSENTER, null),
            Arguments.of(OrgEnhetsType.KLAGEINSTANS, null),
            Arguments.of(OrgEnhetsType.FYLKE, null),
            Arguments.of(OrgEnhetsType.NAV_KONTOR, null),
            Arguments.of(OrgEnhetsType.TILTAK, null),
            Arguments.of(OrgEnhetsType.ARBEIDSLIVSSENTER, null),
            Arguments.of(OrgEnhetsType.ARBEIDSRAADGIVNING, null)
        );
    }

    @Test
    @DisplayName("updateNomNivaaWhenOrgEnhetsTypeIsSet() should not send event if no change in nomNivaa")
    public void mapOrgEnhetTypeToNewNomNivaaAndUpdate_should_not_send_event_if_no_change_in_nom_nivaa(){

        var orgeEnhetState = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .nomNivaa(Optional.of(NomNivaa.DRIFTSENHET))
            .build();

        when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(eq(orgeEnhetState)))
            .thenReturn(Optional.empty());

        orgEnhetStateRepository.update(orgeEnhetState);

        orgEnhetNomNivaaHandler.mapOrgEnhetTypeToNewNomNivaaAndUpdate(
            orgeEnhetState,
            Optional.of(OrgEnhetsType.DIREKTORAT)
        );

        verify(orgEnhetCommandService, times(0))
            .endreNomNivaa(any(), any());

    }

    @Test
    @DisplayName("findAllPathsFromLeafToNode should find all subpaths from a leaf to a node")
    public void findAllPathsFromLeafToNode_should_find_all_leaves() {
        var topNode = orgEnhetState().toBuilder().id("topNode").build();

        var level1_Child1 = orgEnhetState().toBuilder().id("level1_Child").build();
        var level2_Child1_OnlyChild = orgEnhetState().toBuilder().id("level2_Child1_OnlyChild").build();


        var level1_Child2 = orgEnhetState().toBuilder().id("level1_Child2").build();

        //leaves
        var level1_Child3 = orgEnhetState().toBuilder().id("level1_Child3").build();
        var level2_Child2_Child1 = orgEnhetState().toBuilder().id("level2_Child2_Child1").build();
        var level2_Child2_Child2 = orgEnhetState().toBuilder().id("level2_Child2_Child2").build();
        var level3_Child1_Child_Child1 = orgEnhetState().toBuilder().id("level3_Child1_Child_Child1").build();
        var level3_Child1_Child_Child2 = orgEnhetState().toBuilder().id("level3_Child1_Child_Child2").build();

        mockChildren(topNode, List.of(level1_Child1, level1_Child2, level1_Child3));

        mockChildren(level1_Child1, List.of(level2_Child1_OnlyChild));

        mockChildren(level1_Child2, List.of(level2_Child2_Child1, level2_Child2_Child2));

        mockChildren(level2_Child1_OnlyChild, List.of(level3_Child1_Child_Child1, level3_Child1_Child_Child2));

        assertThat(orgEnhetNomNivaaHandler.findAllLeavesFromNode(topNode, new ArrayList<>()))
            .containsExactlyInAnyOrder(level1_Child3, level2_Child2_Child1, level2_Child2_Child2, level3_Child1_Child_Child1, level3_Child1_Child_Child2);

        assertThat(orgEnhetNomNivaaHandler.findAllPathsFromLeafToNode(topNode))
            .containsExactlyInAnyOrder(
                Map.entry(level1_Child3, List.of(level1_Child3, topNode)),
                Map.entry(level2_Child2_Child1, List.of(level2_Child2_Child1, level1_Child2, topNode)),
                Map.entry(level2_Child2_Child2, List.of(level2_Child2_Child2, level1_Child2, topNode)),
                Map.entry(level3_Child1_Child_Child1, List.of(level3_Child1_Child_Child1, level2_Child1_OnlyChild, level1_Child1, topNode)),
                Map.entry(level3_Child1_Child_Child2, List.of(level3_Child1_Child_Child2, level2_Child1_OnlyChild, level1_Child1, topNode))
            );
    }

    @Test
    @DisplayName("addArbeidsomraadeInAllSubPathsIfMissing should update second bottom node with ARBEIDSOMRAADE if no ARBEIDSOMRAADE present in path")
    public void addArbeidsomraadeInAllSubPathsIfMissing_should_update_if_ARBEIDSOMRAADE_missing_in_path() {
        var topNode = orgEnhetState().toBuilder()
            .id("topNode")
            .nomNivaa(Optional.of(NomNivaa.DRIFTSENHET))
            .build();

        var nodeInPathWithNoArbeidsomraade = orgEnhetState().toBuilder().id("nodeInPathWithNoArbeidsomraade").nomNivaa(Optional.empty()).build();
        var arbeidsomraade = orgEnhetState().toBuilder().id("arbeidsomraade").nomNivaa(Optional.of(NomNivaa.ARBEIDSOMRAADE)).build();

        var secondBottomInPathWithNoArbeidsomraade = orgEnhetState().toBuilder().id("secondBottomInPathWithNoArbeidsomraade").nomNivaa(Optional.empty()).build();

        //leaves
        var leafInPathWithArbeidsomraade = orgEnhetState().toBuilder().id("leafInPathWithArbeidsomraade").nomNivaa(Optional.empty()).build();
        var leafWithNoArbeidsomraadeInPath = orgEnhetState().toBuilder().id("leafWithNoArbeidsomraadeInPath").nomNivaa(Optional.empty()).build();

        mockChildren(topNode, List.of(nodeInPathWithNoArbeidsomraade, arbeidsomraade));

        mockChildren(nodeInPathWithNoArbeidsomraade, List.of(secondBottomInPathWithNoArbeidsomraade));

        mockChildren(arbeidsomraade, List.of(leafInPathWithArbeidsomraade));

        mockChildren(secondBottomInPathWithNoArbeidsomraade, List.of(leafWithNoArbeidsomraadeInPath));


        orgEnhetNomNivaaHandler.addArbeidsomraadeInAllSubPathsIfMissing(topNode);

        var fidArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(orgEnhetCommandService).endreNomNivaa(fidArgumentCaptor.capture(), endreNomNivaaArgumentCaptor.capture());

        assertThat(fidArgumentCaptor.getAllValues()).containsOnly(secondBottomInPathWithNoArbeidsomraade.fid());

        assertThat(endreNomNivaaArgumentCaptor.getAllValues())
            .allMatch(endreNomNivaa -> endreNomNivaa.nomNivaa().equals(Optional.of(NomNivaa.ARBEIDSOMRAADE)));
    }

    @Test
    @DisplayName("addArbeidsomraadeInAllSubPathsIfMissing should only update second bottom node with ARBEIDSOMRAADE if node is second bottom in all paths")
    public void addArbeidsomraadeInAllSubPathsIfMissing_should_only_update_to_ARBEIDSOMRAADE_if_node_is_second_bottom_in_some_paths() {
        var topNode = orgEnhetState().toBuilder()
            .id("topNode")
            .nomNivaa(Optional.of(NomNivaa.DRIFTSENHET))
            .build();

        var nodeThatIsSecondAndThirdBottomInDifferentPaths = orgEnhetState().toBuilder().id("nodeInPathWithNoArbeidsomraade").nomNivaa(Optional.empty()).build();

        var secondBottomInPathWithNoArbeidsomraade = orgEnhetState().toBuilder().id("secondBottomInPathWithNoArbeidsomraade").nomNivaa(Optional.empty()).build();

        //leaves
        var leafWithNoArbeidsomraadeInPath = orgEnhetState().toBuilder().id("leafWithNoArbeidsomraadeInPath").nomNivaa(Optional.empty()).build();
        var anotherLeafWithNoArbeidsomraadeInPath = orgEnhetState().toBuilder().id("leafWithNoArbeidsomraadeInPath").nomNivaa(Optional.empty()).build();

        mockChildren(topNode, List.of(nodeThatIsSecondAndThirdBottomInDifferentPaths));

        mockChildren(nodeThatIsSecondAndThirdBottomInDifferentPaths, List.of(secondBottomInPathWithNoArbeidsomraade, anotherLeafWithNoArbeidsomraadeInPath));

        mockChildren(secondBottomInPathWithNoArbeidsomraade, List.of(leafWithNoArbeidsomraadeInPath));


        orgEnhetNomNivaaHandler.addArbeidsomraadeInAllSubPathsIfMissing(topNode);

        var fidArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(orgEnhetCommandService).endreNomNivaa(fidArgumentCaptor.capture(), endreNomNivaaArgumentCaptor.capture());

        assertThat(fidArgumentCaptor.getAllValues()).containsOnly(secondBottomInPathWithNoArbeidsomraade.fid());

    }

    private void mockChildren(OrgEnhetState parent, List<OrgEnhetState> children) {
        when(orgEnhetUnderordnetResolver.findUnderordnedeOrgEnheter(parent))
            .thenReturn(children);

        children.forEach(child ->
                when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(child))
                    .thenReturn(Optional.of(parent))
            );
    }

    private OrgEnhetsType orgEnhetsTypeDifferentFrom(OrgEnhetsType orgEnhetsType) {
        if (orgEnhetsType.equals(OrgEnhetsType.ARBEIDSRAADGIVNING)) {
            return OrgEnhetsType.FYLKE;
        }
        return OrgEnhetsType.ARBEIDSRAADGIVNING;
    }

    private NomNivaa nomNivaaDifferentFrom(NomNivaa nomNivaa) {
        if (nomNivaa != null && nomNivaa.equals(NomNivaa.ARBEIDSOMRAADE)) {
            return NomNivaa.DRIFTSENHET;
        }
        return NomNivaa.ARBEIDSOMRAADE;
    }
}
