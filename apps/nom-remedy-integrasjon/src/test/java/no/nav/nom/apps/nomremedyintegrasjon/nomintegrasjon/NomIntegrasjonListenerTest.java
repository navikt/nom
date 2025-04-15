package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import no.nav.nom.apps.nomremedyintegrasjon.TestBase;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningDagligOppfolgingEndretEventV1;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningState;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursOpprettetEventV1;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursState;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class NomIntegrasjonListenerTest extends TestBase {

    @MockitoBean
    NomRemedyIntegrasjonServiceOld nomRemedyIntegrasjonServiceOld;

    @Autowired
    private KafkaTemplate<String, RessursEvent> ressursEventKafkaTemplate;

    @Autowired
    private KafkaTemplate<String, OrgtilknytningEvent> orgtilknytningEventKafkaTemplate;

    @MockitoBean
    MasteredRessursStateRepository masteredRessursStateRepository;

    @MockitoBean
    RessursStateRepository ressursStateRepository;

    @MockitoBean
    OrgtilknytningStateRepository orgtilknytningStateRepository;

    @MockitoBean
    NomStateMasteredRessursListener nomStateMasteredRessursListener;

    @MockitoBean
    NomStateRessursListener nomStateRessursListener;

    @MockitoBean
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener;

    @MockitoBean
    NomStateOrgEnhetListener nomStateOrgEnhetListener;

    @BeforeEach
    void setUp() {
        when(nomStateMasteredRessursListener.hasReadToEndOnStartup()).thenReturn(true);
        when(nomStateRessursListener.hasReadToEndOnStartup()).thenReturn(true);
        when(nomStateOrgtilknytningListener.hasReadToEndOnStartup()).thenReturn(true);
        when(nomStateOrgEnhetListener.hasReadToEndOnStartup()).thenReturn(true);
    }


    @Test
    @DisplayName("Should handle ressurs events with Originiation check")
    void testRessursEventListenerWithNomOrigination() {
        var ressursOpprettetEventV1 = ressursOpprettetEventV1(new Origination("", "context:namespace:nom-ui2"));
        var ressursState = ressursState(ressursOpprettetEventV1);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        when(ressursStateRepository.findByFid(ressursOpprettetEventV1.getFid())).thenReturn(Optional.of(ressursState));
        when(masteredRessursStateRepository.isMastered(ressursState.navident())).thenReturn(false);

        doAnswer(invocation -> {
            countDownLatch.countDown();
            return null;
        }).when(nomRemedyIntegrasjonServiceOld).handleRessursEvent(ressursOpprettetEventV1);

        ressursEventKafkaTemplate.send(
            ressursEventSourceTopic, UUID.randomUUID().toString(), ressursOpprettetEventV1
        );

        Awaitility.await().until(() -> ressursStateRepository.findByFid(ressursOpprettetEventV1.getFid()).isPresent());
        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
        verify(nomRemedyIntegrasjonServiceOld, times(1))
            .handleRessursEvent(any(RessursOpprettetEventV1.class))
        );
    }

    @Test
    @DisplayName("Should handle ressurs events with nom-mastered check")
    void testRessursEventListenerNomMastered() {
        var ressursOpprettetEventV1 = ressursOpprettetEventV1(new Origination("", "context:namespace:not-nom-ui2"));
        var ressursState = ressursState(ressursOpprettetEventV1);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        when(ressursStateRepository.findByFid(ressursOpprettetEventV1.getFid())).thenReturn(Optional.of(ressursState));
        when(masteredRessursStateRepository.isMastered(ressursState.navident())).thenReturn(true);

        doAnswer(invocation -> {
            countDownLatch.countDown();
            return null;
        }).when(nomRemedyIntegrasjonServiceOld).handleRessursEvent(ressursOpprettetEventV1);

        ressursEventKafkaTemplate.send(
            ressursEventSourceTopic, UUID.randomUUID().toString(), ressursOpprettetEventV1
        );

        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
            verify(nomRemedyIntegrasjonServiceOld, times(1))
            .handleRessursEvent(any(RessursOpprettetEventV1.class))
        );
    }

    @Test
    @DisplayName("Should handle orgtilknytning events with Originiation check")
    void testOrgtilknytningEventListener() {
        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1(new Origination("", "context:namespace:nom-ui2"));
        var orgtilknytningState = orgtilknytningState(orgtilknytningDagligOppfolgingEndretEventV1);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        when(orgtilknytningStateRepository.findByFid(orgtilknytningDagligOppfolgingEndretEventV1.getFid())).thenReturn(Optional.of(orgtilknytningState));
        when(masteredRessursStateRepository.isMastered(anyString())).thenReturn(false);

        doAnswer(invocation -> {
            countDownLatch.countDown();
            return null;
        }).when(nomRemedyIntegrasjonServiceOld).handleOrgtilknytningEvent(orgtilknytningDagligOppfolgingEndretEventV1);

        orgtilknytningEventKafkaTemplate.send(
            orgtilknytningEventSourceTopic, UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEventV1
        );

        Awaitility.await().until(() -> orgtilknytningStateRepository.findByFid(orgtilknytningDagligOppfolgingEndretEventV1.getFid()).isPresent());
        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
            verify(nomRemedyIntegrasjonServiceOld, times(1))
                .handleOrgtilknytningEvent(any(OrgtilknytningEvent.class))
        );
    }

    @Test
    @DisplayName("Should handle orgtilknytning events with nom-mastered check")
    void testOrgtilknytningEventListenerNomMastered() {
        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1(new Origination("", "context:namespace:not-nom-ui2"));
        var orgtilknytningState = orgtilknytningState(orgtilknytningDagligOppfolgingEndretEventV1);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        when(orgtilknytningStateRepository.findByFid(orgtilknytningDagligOppfolgingEndretEventV1.getFid())).thenReturn(Optional.of(orgtilknytningState));
        when(masteredRessursStateRepository.isMastered(anyString())).thenReturn(true);

        doAnswer(invocation -> {
            countDownLatch.countDown();
            return null;
        }).when(nomRemedyIntegrasjonServiceOld).handleOrgtilknytningEvent(orgtilknytningDagligOppfolgingEndretEventV1);

        orgtilknytningEventKafkaTemplate.send(
            orgtilknytningEventSourceTopic, UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEventV1
        );

        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
            verify(nomRemedyIntegrasjonServiceOld, times(1))
                .handleOrgtilknytningEvent(any(OrgtilknytningEvent.class))
        );
    }

    @Test
    @DisplayName("Should skip handling events that are not mastered and not from nom-ui2")
    void testSkipsHandlingEventsIfNotNomMasteredAndNomOrigination() throws InterruptedException {

        var orgtilknytningState = mock(OrgtilknytningState.class);
        when(orgtilknytningStateRepository.findByFid(any())).thenReturn(Optional.of(orgtilknytningState));
        when(masteredRessursStateRepository.isMastered(any())).thenReturn(false);

        var ressursState = mock(RessursState.class);
        when(ressursStateRepository.findByFid(any())).thenReturn(Optional.of(ressursState));
        when(masteredRessursStateRepository.isMastered(any())).thenReturn(false);

        orgtilknytningEventKafkaTemplate.send(
            orgtilknytningEventSourceTopic,
            UUID.randomUUID().toString(),
            orgtilknytningDagligOppfolgingEndretEventV1(new Origination("", "context:namespace:not-nom-ui2"))
        );

        ressursEventKafkaTemplate.send(
            ressursEventSourceTopic,
            UUID.randomUUID().toString(),
            ressursOpprettetEventV1(new Origination("", "context:namespace:not-nom-ui2"))
        );

        Thread.sleep(700);

        verifyNoInteractions(nomRemedyIntegrasjonServiceOld);
    }
}