package no.nav.nom.apps.api.authorize.eventsourcing;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.eventsourcing.state.DelegertLederEventListener;
import no.nav.nom.contract.nom.eventsource.Event;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(classes = TestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {"${app.kafka.authorize.eventsource-topic}"}, controlledShutdown = true)
@ConfigurationPropertiesScan
public class KafkaTestBase {
    @Autowired
    DelegertLederEventListener delegertLederEventListener;

    private Phaser phaser;
    private final AtomicInteger countMessagesReceived = new AtomicInteger();

    @BeforeEach
    void setup() {
        phaser = new Phaser(1);
        delegertLederEventListener.register(new BasicSendReceiveDelegertLederEventsTest.messageReceivedVisitor());
        countMessagesReceived.set(0);
    };

    void registerMessagesToSend(int ticks) {
        phaser.bulkRegister(ticks);
    }

    void tickMessageReceived() {
        countMessagesReceived.incrementAndGet();
        phaser.arriveAndDeregister();
    }

    void awaitAllMessages() throws InterruptedException, TimeoutException {
        phaser.awaitAdvanceInterruptibly(phaser.arrive(), 5, TimeUnit.SECONDS);
        log.info("All messages received - {}", countMessagesReceived);
    }

    public class messageReceivedVisitor implements DelegertLederEventListener.Visitor {
        @Override
        public void visit(Event evt) {
            tickMessageReceived();
        }
    }
}
