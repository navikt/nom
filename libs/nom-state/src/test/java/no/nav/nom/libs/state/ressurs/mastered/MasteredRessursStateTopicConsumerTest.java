package no.nav.nom.libs.state.ressurs.mastered;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(
    classes = {
        NomStateMasteredRessursListener.class
        , PropertiesAutoConfig.class
        , MasteredRessursStateAutoconfiguration.class
        , KafkaProperties.class
        , ObjectMapper.class
    }
)
@Import(KafkaTestFactoryConfiguration.class)
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {"org.nom.nom-mastered-ressurs"})
@EnableKafka
public class MasteredRessursStateTopicConsumerTest {

    private CountDownLatch alleMeldingerLest;

    @Autowired
    NomStateMasteredRessursListener nomStateMasteredRessursListener;

    @Autowired
    MasteredRessursStateRepository masteredRessursStateRepository;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    KafkaTemplate<String, LocalDateTime> masteredRessursStateKafkaTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("Test that we can consume messages from org.nom.nom-mastered-ressurs")
    public void testConsumeMessages() throws InterruptedException {
        setMessageCount(3);

        sendMasteredRessursState(
            "A000001",
            LocalDateTime.now()
        );

        sendMasteredRessursState(
            "A000002",
            LocalDateTime.now()
        );

        sendMasteredRessursState(
            "A000002",
            null
        );

        alleMeldingerLest.await();

        Awaitility.await().until(() -> nomStateMasteredRessursListener.hasReadToEndOnStartup());

        assertEquals(1, masteredRessursStateRepository.count());
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
            nomStateMasteredRessursListener.register(msgVisitor);
            registeredVisitor = true;
        }
    }

    NomStateMasteredRessursListener.Visitor msgVisitor = new NomStateMasteredRessursListener.Visitor<LocalDateTime>() {
        @Override
        public String visitorName() {
            return "TestVisitor";
        }

        @Override
        public void visit(String navident, LocalDateTime since) {
            log.info("Visitor received message: {}-{}", navident, since);
            alleMeldingerLest.countDown();
        }
    };

    public void setMessageCount(int count) {
        alleMeldingerLest = new CountDownLatch(count);
    }

    private SendResult<String, LocalDateTime> sendMasteredRessursState(String navident, LocalDateTime since) {
        log.info("Sending message: {}-{}", navident, since);
        var future = masteredRessursStateKafkaTemplate.send("org.nom.nom-mastered-ressurs", navident, since);
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
