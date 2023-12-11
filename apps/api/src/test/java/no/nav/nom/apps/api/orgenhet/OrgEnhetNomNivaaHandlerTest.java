package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreNomNivaa;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
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

import java.util.Optional;
import java.util.stream.Stream;

import static no.nav.nom.apps.api.common.TestData.orgEnhetState;
import static no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils.orgEnhetStateBuilderWithDefaultValues;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrgEnhetNomNivaaHandlerTest extends TestBase {

    @MockBean
    private OrgEnhetCommandService orgEnhetCommandService;

    @MockBean OrgEnhetOverordnetResolver orgEnhetOverordnetResolver;

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

        orgEnhetStateRepository.putEventAggregate(orgEnhetState);

        when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(orgEnhetState))
            .thenReturn(Optional.of(overordnetOrgEnhetState));

        when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(overordnetOrgEnhetState))
            .thenReturn(Optional.empty());

        orgEnhetNomNivaaHandler.mapOrgEnhetTypeToNewNomNivaaAndUpdate(
            new OrgEnhetTypeEndretEventV1(
                orgEnhetState.fid(),
                new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(
                    Optional.of(orgEnhetsType)
                ),
                new InputMetadata(null,null,null,null)
            )
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

        orgEnhetStateRepository.putEventAggregate(orgEnhetState);

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
            new OrgEnhetTypeEndretEventV1(
                orgEnhetState.fid(),
                new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(
                    Optional.of(orgEnhetsType)
                ),
                new InputMetadata(null,null,null,null)
            )
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

        orgEnhetStateRepository.putEventAggregate(targetOrgEnhetState);

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
            new OrgEnhetTypeEndretEventV1(
                targetOrgEnhetState.fid(),
                new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(
                    Optional.of(orgEnhetsType)
                ),
                new InputMetadata(null,null,null,null)
            )
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

        var orgeEnhetState = orgEnhetStateBuilderWithDefaultValues()
            .nomNivaa(Optional.of(NomNivaa.DRIFTSENHET))
            .build();

        when(orgEnhetOverordnetResolver.findOverordnetOrgEnhet(eq(orgeEnhetState)))
            .thenReturn(Optional.empty());

        orgEnhetStateRepository.putEventAggregate(orgeEnhetState);

        orgEnhetNomNivaaHandler.mapOrgEnhetTypeToNewNomNivaaAndUpdate(
            new OrgEnhetTypeEndretEventV1(
                orgeEnhetState.fid(),
                new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(
                    Optional.of(OrgEnhetsType.DIREKTORAT)
                ),
                new InputMetadata(null,null,null,null)
            )
        );

        verify(orgEnhetCommandService, times(0))
            .endreNomNivaa(any(), any());

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
