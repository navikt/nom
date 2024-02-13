package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

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


public class OrgEnhetTypeHandlerTest extends TestBase {

    @Autowired
    private OrgEnhetStateRepository orgEnhetStateRepository;

    @Autowired
    private OrgEnhetTypeHandler orgEnhetTypeHandler;

    @MockBean
    private OrgEnhetCommandService orgEnhetCommandService;

    @MockBean
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
                    .agressoOrgNivaa(Optional.of("ORGENHET"))
                    .agressoOrgenhetType(Optional.of(agressoOrgenhetType))
                    .build()
            )
            .forEach(orgEnhetState -> orgEnhetStateRepository.putEventAggregate(orgEnhetState));

        orgEnhetTypeHandler.updateOrgEnhetsTypeForLeafs();

        verify(orgEnhetCommandService, times(allAgressoOrgenhetTyper.size()))
            .endreOrgEnhetsType(any(), endreOrgEnhetsTypeArgumentCaptor.capture());

        assertThat(endreOrgEnhetsTypeArgumentCaptor.getAllValues())
            .hasSize(allAgressoOrgenhetTyper.size())
            .map(event -> event.orgEnhetsType().orElse(null))
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
                .agressoOrgNivaa(Optional.of("ORGENHET"))
                .agressoOrgenhetType(Optional.of("Doksenter"))
                .build(),
            orgEnhetStateBuilderWithDefaultValues().toBuilder()
                .agressoOrgNivaa(Optional.of("ORGENHET"))
                .agressoOrgenhetType(Optional.of("Fylke"))
                .build()
        );

        var nonLeafNodes = List.of(
            orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .agressoOrgNivaa(Optional.of("ORGNIV3"))
            .agressoOrgenhetType(Optional.of("HMS"))
            .build()
        );

        Stream.of(nonLeafNodes, leafNodes).flatMap(List::stream)
            .forEach(orgEnhetState -> orgEnhetStateRepository.putEventAggregate(orgEnhetState));

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
            .agressoOrgNivaa(Optional.of("ORGENHET"))
            .agressoOrgenhetType(Optional.of("Fylke"))
            .orgEnhetsType(Optional.of(OrgEnhetsType.FYLKE))
            .build();

        orgEnhetStateRepository.putEventAggregate(orgEnhetWithCorrectOrgenhetType);

        orgEnhetTypeHandler.updateOrgEnhetsTypeForLeafs();

        verify(orgEnhetCommandService, times(0)).endreOrgEnhetsType(any(), any());

    }

    @Test
    @DisplayName("updateOrgEnhetsTypeForLeafs() should send event if incorrect orgenhetType is set")
    public void updateOrgEnhetsTypeForLeafs_should_send_event_if_incorrect_orgenhetType_is_set() {

        var orgEnhetWithCorrectOrgenhetType = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .agressoOrgNivaa(Optional.of("ORGENHET"))
            .agressoOrgenhetType(Optional.of("Fylke"))
            .orgEnhetsType(Optional.of(OrgEnhetsType.NAV_KONTOR))
            .build();

        orgEnhetStateRepository.putEventAggregate(orgEnhetWithCorrectOrgenhetType);

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
                .orgEnhetsType(Optional.of(newOrgEnhetType))
                .build(),
            orgEnhetStateBuilderWithDefaultValues().toBuilder()
                .orgEnhetsType(Optional.of(newOrgEnhetType))
                .build()
        );

        var overordnetOrgEnhet = orgEnhetStateBuilderWithDefaultValues();

        Stream.of(List.of(overordnetOrgEnhet), changedOrgenhetAndSiblings).flatMap(List::stream)
                .forEach(orgEnhetState -> orgEnhetStateRepository.putEventAggregate(orgEnhetState));

        changedOrgenhetAndSiblings.forEach(orgEnhetState ->
            when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(orgEnhetState)).thenReturn(Optional.of(overordnetOrgEnhet))
        );

        orgEnhetTypeHandler.orgEnhetsTypeChanged(
            new OrgEnhetTypeEndretEventV1(
                changedOrgenhet.fid(),
                new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(Optional.of(newOrgEnhetType)),
                new InputMetadata(null, null, null, null)
            )
        );

        verify(orgEnhetCommandService, times(1))
            .endreOrgEnhetsType(eq(overordnetOrgEnhet.fid()), endreOrgEnhetsTypeArgumentCaptor.capture());

        assertThat(endreOrgEnhetsTypeArgumentCaptor.getValue().orgEnhetsType()).isEqualTo(Optional.of(newOrgEnhetType));

    }

    @Test
    @DisplayName("orgEnhetsTypeChanged() should not send an OrgEnhetTypeEndretEventV1 if its overordnet orgEnhetsType is already set")
    public void orgEnhetsTypeChanged_should_not_send_an_OrgEnhetTypeEndretEventV1_if_its_overordnet_orgEnhetsType_is_already_set() {
        var newOrgEnhetType = OrgEnhetsType.FYLKE;
        var changedOrgenhet = orgEnhetStateBuilderWithDefaultValues();

        var overordnetOrgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .orgEnhetsType(Optional.of(OrgEnhetsType.ARBEIDSLIVSSENTER))
            .build();

        Stream.of(List.of(changedOrgenhet), List.of(overordnetOrgEnhet)).flatMap(List::stream)
            .forEach(orgEnhetState -> orgEnhetStateRepository.putEventAggregate(orgEnhetState));

        when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(changedOrgenhet)).thenReturn(Optional.of(overordnetOrgEnhet));

        orgEnhetTypeHandler.orgEnhetsTypeChanged(
            new OrgEnhetTypeEndretEventV1(
                changedOrgenhet.fid(),
                new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(Optional.of(newOrgEnhetType)),
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
                .orgEnhetsType(Optional.of(OrgEnhetsType.FYLKE))
                .build()
        );

        var overordnetOrgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .orgEnhetsType(Optional.empty())
            .build();

        Stream.of(changedOrgenhetAndSiblings, List.of(overordnetOrgEnhet)).flatMap(List::stream)
            .forEach(orgEnhetState -> orgEnhetStateRepository.putEventAggregate(orgEnhetState));

        changedOrgenhetAndSiblings.forEach(orgEnhetState ->
            when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(orgEnhetState)).thenReturn(Optional.of(overordnetOrgEnhet))
        );

        orgEnhetTypeHandler.orgEnhetsTypeChanged(
            new OrgEnhetTypeEndretEventV1(
                changedOrgenhet.fid(),
                new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(Optional.of(OrgEnhetsType.DIREKTORAT)),
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
                .orgEnhetsType(Optional.of(OrgEnhetsType.DIREKTORAT))
                .build()
        );

        var overordnetOrgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .orgEnhetsType(Optional.of(OrgEnhetsType.DIR)).build();

        Stream.of(changedOrgenhetAndSiblings, List.of(overordnetOrgEnhet)).flatMap(List::stream)
            .forEach(orgEnhetState -> orgEnhetStateRepository.putEventAggregate(orgEnhetState));

        changedOrgenhetAndSiblings.forEach(orgEnhetState ->
            when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(orgEnhetState)).thenReturn(Optional.of(overordnetOrgEnhet))
        );

        orgEnhetTypeHandler.orgEnhetsTypeChanged(
            new OrgEnhetTypeEndretEventV1(
                changedOrgenhet.fid(),
                new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(Optional.of(OrgEnhetsType.DIREKTORAT)),
                new InputMetadata(null, null, null, null)
            )
        );

        verify(orgEnhetCommandService, times(1))
            .endreOrgEnhetsType(eq(overordnetOrgEnhet.fid()), endreOrgEnhetsTypeArgumentCaptor.capture());

        assertThat(endreOrgEnhetsTypeArgumentCaptor.getValue().orgEnhetsType()).isEqualTo(Optional.of(OrgEnhetsType.DIREKTORAT));
    }
}
