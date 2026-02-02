package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils.orgEnhetStateBuilderWithDefaultValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrgEnhetTypeHandlerTest extends OrgEnhetTestBase {

    @Autowired
    private OrgEnhetStateRepository orgEnhetStateRepository;

    @Autowired
    private OrgEnhetTypeHandler orgEnhetTypeHandler;

    @MockitoBean
    private OrgEnhetCommandService orgEnhetCommandService;

    @MockitoBean
    OrgEnhetOverordnetResolver orgEnhetOverordnetResolver;

    @Captor
    ArgumentCaptor<EndreOrgEnhetsType> endreOrgEnhetsTypeArgumentCaptor;

    @Captor
    ArgumentCaptor<String> fidArgumentCaptor;

    @BeforeEach
    public void init() {
        orgEnhetStateRepository.clear();
    }

    @Test
    @DisplayName("updateOrgEnhetsTypeForLeafs() should map to all orgEnhetType values correctly")
    public void updateOrgEnhetsTypeForLeafs_should_map_to_all_orgEnhetType_values_correctly() {
        var allAgressoOrgenhetTyper = List.of(
            "ALS",
            "Arbeid og Ytelser",
            "ARK",
            "Dir",
            "Doksenter",
            "Fylke",
            "Rådgivende overlege",
            "Familie- og pensjonsytelser",
            "HMS",
            "Klageinstans",
            "Kontaktsenter",
            "Spes",
            "Lokal",
            "Tiltak",
            "Økonomi"
        );

        // Add leaf nodes (agressoOrgNivaa = "ORGENHET") for all values for agressoOrgenhetType
        allAgressoOrgenhetTyper.stream()
            .map(agressoOrgenhetType ->
                orgEnhetStateBuilderWithDefaultValues().toBuilder()
                    .agressoOrgNivaa("ORGENHET")
                    .agressoOrgenhetType(agressoOrgenhetType)
                    .build()
            )
            .forEach(orgEnhetState -> orgEnhetStateRepository.update(orgEnhetState));

        orgEnhetTypeHandler.updateOrgEnhetsTypeForLeafs();

        verify(orgEnhetCommandService, times(allAgressoOrgenhetTyper.size()))
            .endreOrgEnhetsType(any(), endreOrgEnhetsTypeArgumentCaptor.capture());

        assertThat(endreOrgEnhetsTypeArgumentCaptor.getAllValues())
            .hasSize(allAgressoOrgenhetTyper.size())
            .map(EndreOrgEnhetsType::orgEnhetsType)
            // Assert that events are sent for all OrgEnhetsType values except DIR (legacy code that gets mapped to DIREKTORAT)
            .containsAll(Arrays.stream(OrgEnhetsType.values())
                .filter(orgEnhetsType -> !orgEnhetsType.equals(OrgEnhetsType.DIR)).toList()
            );


    }

    @Test
    @DisplayName("updateOrgEnhetsTypeForLeafs() should send events for leaf nodes only")
    public void updateOrgEnhetsTypeForLeafs_should_send_events_for_leaf_nodes_only() {

        var leafNodes = List.of(
            orgEnhetStateBuilderWithDefaultValues().toBuilder()
                .agressoOrgNivaa("ORGENHET")
                .agressoOrgenhetType("Doksenter")
                .build(),
            orgEnhetStateBuilderWithDefaultValues().toBuilder()
                .agressoOrgNivaa("ORGENHET")
                .agressoOrgenhetType("Fylke")
                .build()
        );

        var nonLeafNodes = List.of(
            orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .agressoOrgNivaa("ORGNIV3")
            .agressoOrgenhetType("HMS")
            .build()
        );

        Stream.of(nonLeafNodes, leafNodes).flatMap(List::stream)
            .forEach(orgEnhetState -> orgEnhetStateRepository.update(orgEnhetState));

        orgEnhetTypeHandler.updateOrgEnhetsTypeForLeafs();

        verify(orgEnhetCommandService, times(leafNodes.size()))
            .endreOrgEnhetsType(fidArgumentCaptor.capture(), any());

        assertThat(fidArgumentCaptor.getAllValues())
            .containsExactlyInAnyOrderElementsOf(leafNodes.stream().map(OrgEnhetState::fid).toList());
    }

    @Test
    @DisplayName("updateOrgEnhetsTypeForLeafs() should not send event if correct orgenhetType is already set")
    public void updateOrgEnhetsTypeForLeafs_should_not_send_event_if_correct_orgenhetType_is_already_set() {

        var orgEnhetWithCorrectOrgenhetType = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .agressoOrgNivaa("ORGENHET")
            .agressoOrgenhetType("Fylke")
            .orgEnhetsType(OrgEnhetsType.FYLKE)
            .build();

        orgEnhetStateRepository.update(orgEnhetWithCorrectOrgenhetType);

        orgEnhetTypeHandler.updateOrgEnhetsTypeForLeafs();

        verify(orgEnhetCommandService, times(0)).endreOrgEnhetsType(any(), any());

    }

    @Test
    @DisplayName("updateOrgEnhetsTypeForLeafs() should send event if incorrect orgenhetType is set")
    public void updateOrgEnhetsTypeForLeafs_should_send_event_if_incorrect_orgenhetType_is_set() {

        var orgEnhetWithCorrectOrgenhetType = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .agressoOrgNivaa("ORGENHET")
            .agressoOrgenhetType("Fylke")
            .orgEnhetsType(OrgEnhetsType.NAV_KONTOR)
            .build();

        orgEnhetStateRepository.update(orgEnhetWithCorrectOrgenhetType);

        orgEnhetTypeHandler.updateOrgEnhetsTypeForLeafs();

        verify(orgEnhetCommandService, times(1)).endreOrgEnhetsType(any(), any());

    }

    @Test
    @DisplayName("orgEnhetsTypeChanged() should send OrgEnhetTypeEndretEventV1 for overordnet org enhet")
    public void orgEnhetsTypeChanged_should_send_OrgEnhetTypeEndretEventV1_for_overordnet_org_enhet() {

        var newOrgEnhetType = OrgEnhetsType.FYLKE;

        var changedOrgenhet = orgEnhetStateBuilderWithDefaultValues();

        var changedOrgenhetAndSiblings = List.of(
            changedOrgenhet,
            orgEnhetStateBuilderWithDefaultValues().toBuilder()
                .orgEnhetsType(newOrgEnhetType)
                .build(),
            orgEnhetStateBuilderWithDefaultValues().toBuilder()
                .orgEnhetsType(newOrgEnhetType)
                .build()
        );

        var overordnetOrgEnhet = orgEnhetStateBuilderWithDefaultValues();

        Stream.of(List.of(overordnetOrgEnhet), changedOrgenhetAndSiblings).flatMap(List::stream)
                .forEach(orgEnhetState -> orgEnhetStateRepository.update(orgEnhetState));

        changedOrgenhetAndSiblings.forEach(orgEnhetState ->
            when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(orgEnhetState)).thenReturn(Optional.of(overordnetOrgEnhet))
        );

        orgEnhetTypeHandler.orgEnhetsTypeChanged(
            new OrgEnhetTypeEndretEventV1(
                changedOrgenhet.fid(),
                new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(newOrgEnhetType),
                new InputMetadata(null, null, null, null)
            )
        );

        verify(orgEnhetCommandService, times(1))
            .endreOrgEnhetsType(eq(overordnetOrgEnhet.fid()), endreOrgEnhetsTypeArgumentCaptor.capture());

        assertThat(endreOrgEnhetsTypeArgumentCaptor.getValue().orgEnhetsType()).isEqualTo(newOrgEnhetType);

    }

    @Test
    @DisplayName("orgEnhetsTypeChanged() should not send an OrgEnhetTypeEndretEventV1 if its overordnet orgEnhetsType is already set")
    public void orgEnhetsTypeChanged_should_not_send_an_OrgEnhetTypeEndretEventV1_if_its_overordnet_orgEnhetsType_is_already_set() {
        var newOrgEnhetType = OrgEnhetsType.FYLKE;
        var changedOrgenhet = orgEnhetStateBuilderWithDefaultValues();

        var overordnetOrgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .orgEnhetsType(OrgEnhetsType.ARBEIDSLIVSSENTER)
            .build();

        Stream.of(List.of(changedOrgenhet), List.of(overordnetOrgEnhet)).flatMap(List::stream)
            .forEach(orgEnhetState -> orgEnhetStateRepository.update(orgEnhetState));

        when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(changedOrgenhet)).thenReturn(Optional.of(overordnetOrgEnhet));

        orgEnhetTypeHandler.orgEnhetsTypeChanged(
            new OrgEnhetTypeEndretEventV1(
                changedOrgenhet.fid(),
                new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(newOrgEnhetType),
                new InputMetadata(null, null, null, null)
            )
        );

        verify(orgEnhetCommandService, times(0)).endreOrgEnhetsType(any(), any());

    }

    @Test
    @DisplayName("orgEnhetsTypeChanged() should not send an OrgEnhetTypeEndretEventV1 for overordnet if children have different orgEnhetsType")
    public void orgEnhetsTypeChanged_should_not_send_an_OrgEnhetTypeEndretEventV1_for_overordnet_if_children_have_different_orgEnhetsType() {
        var changedOrgenhet = orgEnhetStateBuilderWithDefaultValues();

        var changedOrgenhetAndSiblings = List.of(
            changedOrgenhet,
            orgEnhetStateBuilderWithDefaultValues().toBuilder()
                .orgEnhetsType(OrgEnhetsType.FYLKE)
                .build()
        );

        var overordnetOrgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .orgEnhetsType(null)
            .build();

        Stream.of(changedOrgenhetAndSiblings, List.of(overordnetOrgEnhet)).flatMap(List::stream)
            .forEach(orgEnhetState -> orgEnhetStateRepository.update(orgEnhetState));

        changedOrgenhetAndSiblings.forEach(orgEnhetState ->
            when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(orgEnhetState)).thenReturn(Optional.of(overordnetOrgEnhet))
        );

        orgEnhetTypeHandler.orgEnhetsTypeChanged(
            new OrgEnhetTypeEndretEventV1(
                changedOrgenhet.fid(),
                new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(OrgEnhetsType.DIREKTORAT),
                new InputMetadata(null, null, null, null)
            )
        );

        verify(orgEnhetCommandService, times(0)).endreOrgEnhetsType(any(), any());

    }

    @Test
    @DisplayName("orgEnhetsTypeChanged() should send an OrgEnhetTypeEndretEventV1 for overordnet if its type is set to DIR")
    public void orgEnhetsTypeChanged_should_send_an_OrgEnhetTypeEndretEventV1_for_overordnet_if_its_type_is_set_to_DIR() {
        var changedOrgenhet = orgEnhetStateBuilderWithDefaultValues();

        var changedOrgenhetAndSiblings = List.of(
            changedOrgenhet,
            orgEnhetStateBuilderWithDefaultValues().toBuilder()
                .orgEnhetsType(OrgEnhetsType.DIREKTORAT)
                .build()
        );

        var overordnetOrgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .orgEnhetsType(OrgEnhetsType.DIR).build();

        Stream.of(changedOrgenhetAndSiblings, List.of(overordnetOrgEnhet)).flatMap(List::stream)
            .forEach(orgEnhetState -> orgEnhetStateRepository.update(orgEnhetState));

        changedOrgenhetAndSiblings.forEach(orgEnhetState ->
            when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(orgEnhetState)).thenReturn(Optional.of(overordnetOrgEnhet))
        );

        orgEnhetTypeHandler.orgEnhetsTypeChanged(
            new OrgEnhetTypeEndretEventV1(
                changedOrgenhet.fid(),
                new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(OrgEnhetsType.DIREKTORAT),
                new InputMetadata(null, null, null, null)
            )
        );

        verify(orgEnhetCommandService, times(1))
            .endreOrgEnhetsType(eq(overordnetOrgEnhet.fid()), endreOrgEnhetsTypeArgumentCaptor.capture());

        assertThat(endreOrgEnhetsTypeArgumentCaptor.getValue().orgEnhetsType()).isEqualTo(OrgEnhetsType.DIREKTORAT);
    }
}
