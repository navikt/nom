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

import static no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils.orgEnhetStateBuilderWithDefaultValues;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OrgEnhetNomNivaaHandlerTest extends TestBase {

    @MockBean
    private OrgEnhetCommandService orgEnhetCommandService;

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


    @ParameterizedTest(name = "orgEnhetsType {0} agressoOrgNiva {1} maps to exptectedNomNivaa {2}")
    @MethodSource("updateNomNivaaWhenOrgEnhetsTypeIsSet_should_map_niva_correctly_and_send_event_input")
    public void updateNomNivaaWhenOrgEnhetsTypeIsSet_should_map_niva_correctly_and_send_event(
        OrgEnhetsType orgEnhetsType, String agressoOrgNiva, NomNivaa exptectedNomNivaa
    ) {

        var orgeEnhetState = orgEnhetStateBuilderWithDefaultValues()
            .agressoOrgNivaa(Optional.ofNullable(agressoOrgNiva))
            .build();

        orgEnhetStateRepository.putEventAggregate(orgeEnhetState);

        orgEnhetNomNivaaHandler.updateNomNivaaWhenOrgEnhetsTypeIsSet(
            new OrgEnhetTypeEndretEventV1(
                orgeEnhetState.fid(),
                new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(
                    Optional.ofNullable(orgEnhetsType)
                ),
                new InputMetadata(null,null,null,null)
            )
        );

        verify(orgEnhetCommandService, times(1))
            .endreNomNivaa(any(), endreNomNivaaArgumentCaptor.capture());

        assertThat(endreNomNivaaArgumentCaptor.getValue().nomNivaa()).isEqualTo(Optional.ofNullable(exptectedNomNivaa));
    }

    public static Stream<Arguments> updateNomNivaaWhenOrgEnhetsTypeIsSet_should_map_niva_correctly_and_send_event_input() {
        return Stream.of(
            Arguments.of(OrgEnhetsType.DIREKTORAT, "ORGNIV1", NomNivaa.DRIFTSENHET),
            Arguments.of(OrgEnhetsType.DIREKTORAT, "ORGNIV2", NomNivaa.ARBEIDSOMRAADE),
            Arguments.of(OrgEnhetsType.FYLKE, "ORGNIV1", NomNivaa.LINJEENHET),
            Arguments.of(OrgEnhetsType.KLAGEINSTANS, "ORGNIV2", NomNivaa.DRIFTSENHET),
            Arguments.of(OrgEnhetsType.ARBEIDSLIVSSENTER, "ORGNIV3", NomNivaa.ARBEIDSOMRAADE)
        );
    }

    @Test
    @DisplayName("updateNomNivaaWhenOrgEnhetsTypeIsSet() should not send event if no change in nomNivaa")
    public void updateNomNivaaWhenOrgEnhetsTypeIsSet_should_not_send_event_if_no_change_in_nom_nivaa(){

        var orgeEnhetState = orgEnhetStateBuilderWithDefaultValues()
            .agressoOrgNivaa(Optional.of("ORGNIV1"))
            .nomNivaa(Optional.of(NomNivaa.DRIFTSENHET))
            .build();

        orgEnhetStateRepository.putEventAggregate(orgeEnhetState);

        orgEnhetNomNivaaHandler.updateNomNivaaWhenOrgEnhetsTypeIsSet(
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

}
