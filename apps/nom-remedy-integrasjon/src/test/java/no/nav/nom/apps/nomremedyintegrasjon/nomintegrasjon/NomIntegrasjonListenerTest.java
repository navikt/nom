package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import no.nav.nom.apps.nomremedyintegrasjon.TestBase;

import static no.nav.nom.apps.nomremedyintegrasjon.TestData.orgtilknytningDagligOppfolgingEndretEventV1;
import static no.nav.nom.apps.nomremedyintegrasjon.TestData.ressursOpprettetEventV1;

import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NomIntegrasjonListenerTest extends TestBase {

    @MockBean
    NomIntegrasjonService nomIntegrasjonService;

    @Autowired
    private KafkaTemplate<String, RessursEvent> ressursEventKafkaTemplate;

    @Autowired
    private KafkaTemplate<String, OrgtilknytningEvent> orgtilknytningEventKafkaTemplate;

    @BeforeEach
    void unleashFeaturesEnabled() {
        when(unleash.isEnabled(any())).thenReturn(true);
    }

    @Test
    @DisplayName("Should listen to ressurs events")
    void testRessursEventListener() throws InterruptedException {
        var ressursOpprettetEventV1 = ressursOpprettetEventV1();

        CountDownLatch countDownLatch = new CountDownLatch(1);

        doAnswer(invocation -> {
            countDownLatch.countDown();
            return null;
        }).when(nomIntegrasjonService).handleRessursEvent(any());

        ressursEventKafkaTemplate.send(
            ressursEventSourceTopic, UUID.randomUUID().toString(), ressursOpprettetEventV1
        );

        countDownLatch.await(5, TimeUnit.SECONDS);

        verify(nomIntegrasjonService, times(1))
            .handleRessursEvent(any(RessursOpprettetEventV1.class));
    }

    @Test
    @DisplayName("Should listen to orgtilknytning events")
    void testOrgtilknytningEventListener() throws InterruptedException {
        var orgtilknytningDagligOppfolgingEndretEventV1 = orgtilknytningDagligOppfolgingEndretEventV1();

        CountDownLatch countDownLatch = new CountDownLatch(1);

        doAnswer(invocation -> {
            countDownLatch.countDown();
            return null;
        }).when(nomIntegrasjonService).handleRessursEvent(any());

        orgtilknytningEventKafkaTemplate.send(
            orgtilknytningEventSourceTopic, UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEventV1
        );

        countDownLatch.await(5, TimeUnit.SECONDS);

        verify(nomIntegrasjonService, times(1))
            .handleOrgtilknytningEvent(any(OrgtilknytningEvent.class));
    }
}
