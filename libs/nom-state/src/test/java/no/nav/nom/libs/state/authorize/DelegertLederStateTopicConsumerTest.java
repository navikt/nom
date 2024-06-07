package no.nav.nom.libs.state.authorize;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
import no.nav.nom.contract.nom.eventsource.DelegertAuthorizationAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.DelegertAuthorizationFornyetEventV1;
import no.nav.nom.contract.nom.eventsource.DelegertAuthorizationOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.libs.state.common.PropertiesAutoConfig;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(
    classes = {
        NomStateDelegertLederListener.class
        , PropertiesAutoConfig.class
        , DelegertLederStateAutoconfiguration.class
        , KafkaProperties.class
        , ObjectMapper.class
    }
)
@Import(KafkaTestFactoryConfiguration.class)
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {"org.nom.nom-authorize-eventsource"})
@EnableKafka
public class DelegertLederStateTopicConsumerTest {

    private CountDownLatch alleMeldingerLest = new CountDownLatch(1);

    @Autowired
    NomStateDelegertLederListener nomStateDelegertLederListener;

    @Autowired
    DelegertLederStateRepository delegertLederStateRepository;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    KafkaTemplate<String, AuthorizationEvent> delegertLederStateKafkaTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("Test that we can consume messages from org.nom.nom-authorize-eventsource")
    public void testConsumeMessages() throws InterruptedException {
        setMessageCount(1);

        sendDelegertLederEvt(
            "FID_1",
            new DelegertAuthorizationOpprettetEventV1(
                "FID_1",
                new DelegertAuthorizationOpprettetEventV1.DelegertAuthorizationOpprettetEventData(
                    "A000001",
                    "B0000001",
                    "ab123c",
                    LocalDate.now(),
                    LocalDate.now().plusYears(1)
                ),
                (Event.Metadata) null
            )
        );

        alleMeldingerLest.await();
        assertEquals(LocalDate.now().plusYears(1), delegertLederStateRepository.findByFid("FID_1").get().gyldigTom());

        setMessageCount(1);
        sendDelegertLederEvt(
            "FID_1",
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

        sendDelegertLederEvt(
            "FID_1",
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

        Awaitility.await().until(() -> nomStateDelegertLederListener.hasReadToEndOnStartup());

        assertEquals(1, delegertLederStateRepository.count());
        assertEquals(LocalDate.now().plusYears(1), delegertLederStateRepository.findByFid("FID_1").get().gyldigTom());
    }

    private static boolean registeredVisitor = false;

    @Value("${spring.embedded.kafka.brokers}")
    String kafkabroker;
    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapservers;

    @BeforeEach
    public void registerVisitor() {
        log.info("Kafka broker:      {}", kafkabroker);
        log.info("Bootstrap-servers: {}", bootstrapservers);
        if (!registeredVisitor) {
            nomStateDelegertLederListener.register(msgVisitor);
            registeredVisitor = true;
        }
    }

    NomStateDelegertLederListener.Visitor msgVisitor = new NomStateDelegertLederListener.Visitor<AuthorizationEvent>() {
        @Override
        public String visitorName() {
            return "TestVisitor";
        }

        @Override
        public void visit(String fid, AuthorizationEvent event) {
            log.info("Visitor received message: {}-{}", fid, event);
            alleMeldingerLest.countDown();
        }
    };

    public void setMessageCount(int count) {
        alleMeldingerLest = new CountDownLatch(count);
    }

    private SendResult<String, AuthorizationEvent> sendDelegertLederEvt(String fid, AuthorizationEvent event) {
        log.info("Sending message: {}-{}", fid, event);
        var future = delegertLederStateKafkaTemplate.send("org.nom.nom-authorize-eventsource", fid, event);
        try {
            var result = future.get(1, TimeUnit.SECONDS);
            log.info("Sent message: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Failed to send message", e);
        }
        return null;
    }
}
