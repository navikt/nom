package no.nav.nom.libs.state.ressurs.mastered;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.libs.state.StateTopicConsumerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@Import({
    KafkaTestFactoryConfiguration.class
    , NomStateMasteredRessursListener.class
    , MasteredRessursStateAutoconfiguration.class
})
public class MasteredRessursStateTopicConsumerTest extends StateTopicConsumerTestBase<LocalDateTime> {

    public MasteredRessursStateTopicConsumerTest(NomStateMasteredRessursListener nomStateMasteredRessursListener) {
        super(nomStateMasteredRessursListener);
        stateTopic = "org.nom.nom-mastered-ressurs";

        testMessage = LocalDateTime.now();
    }

    @Autowired
    MasteredRessursStateRepository masteredRessursStateRepository;

    @BeforeEach
    public void emptyRepo() {
        masteredRessursStateRepository.clear();
    }

    @Test
    @DisplayName("Test that we can consume messages from org.nom.nom-mastered-ressurs")
    public void testConsumeMessages() throws InterruptedException {
        setMessageCount(3);

        sendMessage("A000001", testMessage);
        sendMessage("A000002", testMessage);
        sendMessage("A000002", null);

        alleMeldingerLest.await();

        assertEquals(1, masteredRessursStateRepository.count());
    }
}
