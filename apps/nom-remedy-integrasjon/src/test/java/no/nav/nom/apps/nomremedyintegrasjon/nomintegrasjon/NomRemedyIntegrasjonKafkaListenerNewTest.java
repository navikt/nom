package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningStateContract;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursState;

@ExtendWith(MockitoExtension.class)
public class NomRemedyIntegrasjonKafkaListenerNewTest {

    @InjectMocks
    private NomRemedyIntegrasjonKafkaListenerNew nomRemedyIntegrasjonKafkaListenerNew;

    @Mock
    MasteredRessursStateRepository masteredRessursStateRepository;

    @Mock
    NomStateRessursListener nomStateRessursListener;

    @Mock
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener;

    @Mock
    NomStateMasteredRessursListener nomStateMasteredRessursListener;

    @BeforeEach
    void setUp() {
        when(nomStateMasteredRessursListener.hasReadToEndOnStartup()).thenReturn(true);
        when(nomStateRessursListener.hasReadToEndOnStartup()).thenReturn(true);
        when(nomStateOrgtilknytningListener.hasReadToEndOnStartup()).thenReturn(true);
    }

    @Test
    @DisplayName("Test that ressursStateListener logs ressursStateFid")
    void test_that_ressursStateListener_logs_ressursStateFid() {
        // Given
        var ressursState = ressursState();

        // And
        doReturn(true)
            .when(masteredRessursStateRepository)
            .isMastered(ressursState.navident());

        // When
        nomRemedyIntegrasjonKafkaListenerNew.ressursStateListener(ressursState, ressursState.fid());

        // Then
        // Verify that something happened
    }

    @Test
    @DisplayName("Test that orgtilknytningStateListener logs orgtilknytningStateFid")
    void test_that_orgtilknytningStateListener_logs_orgtilknytningStateFid() {
        // Given
        var orgtilknytningState = orgtilknytningStateContract();

        // And
        doReturn(true)
            .when(masteredRessursStateRepository)
            .isMastered(orgtilknytningState.navident());

        // When
        nomRemedyIntegrasjonKafkaListenerNew.orgtilknytningStateListener(orgtilknytningState, orgtilknytningState.fid());

        // Then
        // Verify that something happened
    }
}
