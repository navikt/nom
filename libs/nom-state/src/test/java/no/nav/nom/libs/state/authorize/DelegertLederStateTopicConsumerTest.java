package no.nav.nom.libs.state.authorize;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
import no.nav.nom.contract.nom.eventsource.DelegertAuthorizationAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.DelegertAuthorizationFornyetEventV1;
import no.nav.nom.contract.nom.eventsource.DelegertAuthorizationOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.libs.state.StateTopicConsumerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@Import({
    KafkaTestFactoryConfiguration.class,
    NomStateDelegertLederListener.class,
    DelegertLederStateAutoconfiguration.class
})
public class DelegertLederStateTopicConsumerTest extends StateTopicConsumerTestBase<AuthorizationEvent> {

    public DelegertLederStateTopicConsumerTest(NomStateDelegertLederListener nomStateDelegertLederListener) {
        super(nomStateDelegertLederListener);
        stateTopic = "org.nom.nom-authorize-eventsource";

        testMessage = new DelegertAuthorizationOpprettetEventV1(
            "FID_1",
            new DelegertAuthorizationOpprettetEventV1.DelegertAuthorizationOpprettetEventData(
                "A000001",
                "B0000001",
                "ab123c",
                LocalDate.now(),
                LocalDate.now().plusYears(1)
            ),
            (Event.Metadata) null
        );
    }

    @Autowired
    DelegertLederStateRepository delegertLederStateRepository;

    @BeforeEach
    public void emptyRepo() {
        delegertLederStateRepository.clear();
    }

    @Test
    @DisplayName("Test that we can consume messages from org.nom.nom-authorize-eventsource")
    public void testConsumeMessages() throws InterruptedException {
        setMessageCount(1);

        sendMessage("FID_1", testMessage);

        alleMeldingerLest.await();
        assertEquals(LocalDate.now().plusYears(1), delegertLederStateRepository.findByFid("FID_1").get().gyldigTom());

        setMessageCount(1);
        sendMessage("FID_1",
            new DelegertAuthorizationFornyetEventV1(
                "FID_1",
                new DelegertAuthorizationFornyetEventV1.DelegertAuthorizationFornyetEventData(
                    "A000001",
                    "B0000001",
                    "ab123c",
                    LocalDate.now(),
                    LocalDate.now().plusYears(1),
                    LocalDate.now().plusYears(2)
                ),
                (Event.Metadata) null
            )
        );

        alleMeldingerLest.await();
        assertEquals(LocalDate.now().plusYears(2), delegertLederStateRepository.findByFid("FID_1").get().gyldigTom());

        setMessageCount(1);

        sendMessage("FID_1",
            new DelegertAuthorizationAvsluttetEventV1(
                "FID_1",
                new DelegertAuthorizationAvsluttetEventV1.DelegertAuthorizationAvsluttetEventData(
                    "A000001",
                    "B0000001",
                    "ab123c",
                    LocalDate.now(),
                    LocalDate.now().plusYears(2),
                    LocalDate.now().plusYears(1)
                ),
                (Event.Metadata) null
            )
        );

        alleMeldingerLest.await();

        assertEquals(1, delegertLederStateRepository.count());
        assertEquals(LocalDate.now().plusYears(1), delegertLederStateRepository.findByFid("FID_1").get().gyldigTom());
    }
}
