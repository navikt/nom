package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate.AggregateState;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate.AggregateStateRepository;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningOpprettetEventV1MasseFlytting;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningStateContract;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class NomRemedyIntegrasjonKafkaListenerNewTest {

    @Mock
    MasteredRessursStateRepository masteredRessursStateRepository;
    @Mock
    NomStateRessursListener nomStateRessursListener;
    @Mock
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    @Mock
    NomStateMasteredRessursListener nomStateMasteredRessursListener;
    @Mock
    ObjectMapper objectMapper;
    @Mock
    AggregateStateRepository aggregateStateRepository;
    @InjectMocks
    private NomRemedyIntegrasjonKafkaListenerNew nomRemedyIntegrasjonKafkaListenerNew;

    @BeforeEach
    void setUp() {

    }

    @Test
    @DisplayName("Test that ressursStateListener logs ressursState")
    void test_that_ressursStateListener_saves_ressursState() {
        // Given
        var ressursState = ressursState();

        // And
        doReturn(true)
            .when(masteredRessursStateRepository)
            .isMastered(ressursState.navident());

        doReturn(List.of())
            .when(aggregateStateRepository)
            .findByStateFid(ressursState.fid());

        doReturn(true)
            .when(nomStateRessursListener)
            .hasReadToEndOnStartup();

        doReturn(true)
            .when(nomStateOrgtilknytningListener)
            .hasReadToEndOnStartup();

        doReturn(true)
            .when(nomStateMasteredRessursListener)
            .hasReadToEndOnStartup();

        // When
        nomRemedyIntegrasjonKafkaListenerNew.ressursStateListener(ressursState, ressursState.fid());

        // Then
        verify(aggregateStateRepository, times(1)).save(any(AggregateState.class));
    }

    @Test
    @DisplayName("Test that orgtilknytningStateListener saves orgtilknytningState")
    void test_that_orgtilknytningStateListener_saves_orgtilknytningState() {
        // Given
        var orgtilknytningState = orgtilknytningStateContract();
        var aggregateState = AggregateState.builder()
            .aggregateStateId(UUID.randomUUID())
            .navident(orgtilknytningState.navident())
            .createdAt(LocalDateTime.now())
            .stateFid(orgtilknytningState.fid())
            .stateType(orgtilknytningState.getClass().getSimpleName())
            .build();

        // And
        doReturn(true)
            .when(masteredRessursStateRepository)
            .isMastered(orgtilknytningState.navident());

        doReturn(aggregateState).when(aggregateStateRepository).save(any(AggregateState.class));

        doReturn(List.of(aggregateState))
            .when(aggregateStateRepository)
            .findByStateFid(orgtilknytningState.fid());

        doReturn(true)
            .when(nomStateRessursListener)
            .hasReadToEndOnStartup();

        doReturn(true)
            .when(nomStateOrgtilknytningListener)
            .hasReadToEndOnStartup();

        doReturn(true)
            .when(nomStateMasteredRessursListener)
            .hasReadToEndOnStartup();

        // When
        nomRemedyIntegrasjonKafkaListenerNew.orgtilknytningStateListener(orgtilknytningState, orgtilknytningState.fid());

        // Then
        verify(aggregateStateRepository, times(1)).save(any(AggregateState.class));
        assertEquals(orgtilknytningState.fid(), aggregateStateRepository.findByStateFid(orgtilknytningState.fid()).getFirst().getStateFid());
    }


    @DisplayName("Test that orgtilknytningEventListener saves masseflytting OrgtilknytningOpprettetEventV1")
    void test_that_orgtilknytningEventListener_saves_masseflytting_OrgtilknytningOpprettetEventV1() {
        // Given
        var orgtilknytningOpprettetEventV1 = orgtilknytningOpprettetEventV1MasseFlytting();
        var aggregateState = AggregateState.builder()
            .aggregateStateId(UUID.randomUUID())
            .navident(orgtilknytningOpprettetEventV1.getOrgtilknytningOpprettetEventData().navident())
            .createdAt(LocalDateTime.now())
            .stateFid(orgtilknytningOpprettetEventV1.getFid())
            .stateType(orgtilknytningOpprettetEventV1.getClass().getSimpleName())
            .masseflyttetFraFid(orgtilknytningOpprettetEventV1.getOrgtilknytningOpprettetEventData().masseflyttetFraFid())
            .masseflytting(true)
            .build();

        // And
        doReturn(aggregateState).when(aggregateStateRepository).save(any(AggregateState.class));

        doReturn(Map.of("beholdTilganger", true))
            .when(objectMapper)
            .convertValue(any(), any(TypeReference.class));
        // When
        //nomRemedyIntegrasjonKafkaListenerNew.orgtilknytningEventListener(orgtilknytningOpprettetEventV1, orgtilknytningOpprettetEventV1.getFid());

        // Then
        ArgumentCaptor<AggregateState> captor = ArgumentCaptor.forClass(AggregateState.class);
        verify(aggregateStateRepository, times(1)).save(captor.capture());
        AggregateState savedAggregateState = captor.getValue();

        assertEquals(orgtilknytningOpprettetEventV1.getOrgtilknytningOpprettetEventData().masseflyttetFraFid(), savedAggregateState.getMasseflyttetFraFid());
    }
}
