package no.nav.nom.apps.api.authorize.eventsourcing;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import no.nav.nom.libs.state.common.PropertiesAutoConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;

import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(classes = TestConfig.class,
    properties = {
        "nom.libs.state.idle-event-interval-seconds: 1",
        "nom.libs.state.delegert-leder.enabled: true",
        "nom.libs.state.delegert-leder.listener-id: nom-api-test-listener",
        "nom.libs.state.delegert-leder.state-topic: org.nom.nom-authorize-eventsource"
    })
@EmbeddedKafka(partitions = 1, topics = {"org.nom.nom-authorize-eventsource"}, controlledShutdown = true)
@ConfigurationPropertiesScan
public class KafkaTestBase {
    @Autowired
    NomStateDelegertLederListener delegertLederEventListener;

    private Phaser phaser;
    private final AtomicInteger countMessagesReceived = new AtomicInteger();

    @BeforeEach
    void setup() {
        phaser = new Phaser(1);
        delegertLederEventListener.register( new NomStateDelegertLederListener.Visitor<>() {
            @Override
            public String visitorName() {
                return "KafkaTestBase";
            }

            @Override
            public void visit(String key, AuthorizationEvent evt) {
                tickMessageReceived();
            }
        });
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
}
