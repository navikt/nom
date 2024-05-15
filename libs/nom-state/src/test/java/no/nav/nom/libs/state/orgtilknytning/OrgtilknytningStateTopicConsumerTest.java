package no.nav.nom.libs.state.orgtilknytning;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import no.nav.nom.libs.state.common.PropertiesAutoConfig;
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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(
    classes = {
        NomStateOrgtilknytningListener.class
        , PropertiesAutoConfig.class
        , OrgtilknytningStateAutoconfiguration.class
        , KafkaProperties.class
        , ObjectMapper.class
    }
)
@Import(KafkaTestFactoryConfiguration.class)
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {"org.nom.intern-orgtilknytning-state"}, controlledShutdown = false)
@EnableKafka
public class OrgtilknytningStateTopicConsumerTest {

    private CountDownLatch alleMeldingerLest = new CountDownLatch(1);

    @Autowired
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("Test that we can consume messages from org.nom.intern-orgtilknytning-state")
    public void testConsumeMessages() throws InterruptedException {
        sendOrgtilknytningState(
            "FID_1",
            OrgtilknytningState.builder()
                .fid("FID_1")
                .navident("Z999999")
                .orgEnhetId("ab123c")
                .gyldigFom(LocalDate.now().minusMonths(3))
                .gyldigTom(Optional.empty())
                .erDagligOppfolging(true)
                .orgtilknytningType(Optional.of(Tilknytningtype.INGEN))
                .build()
        );
        alleMeldingerLest.await();
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
            nomStateOrgtilknytningListener.register(msgVisitor);
            registeredVisitor = true;
        }
        resetLatch();
    }

    AbstractNomStateListener.Visitor<OrgtilknytningState> msgVisitor = new NomStateOrgtilknytningListener.Visitor<OrgtilknytningState>() {
        @Override
        public String visitorName() {
            return "TestVisitor";
        }

        @Override
        public void visit(OrgtilknytningState orgtilknytningState) {
            log.info("Visitor received message: {}", orgtilknytningState);
            alleMeldingerLest.countDown();
        }
    };

    public void resetLatch() {
        alleMeldingerLest = new CountDownLatch(1);
    }

    private SendResult<String, OrgtilknytningState> sendOrgtilknytningState(String key, OrgtilknytningState orgtilknytningState) {
        log.info("Sending message: {}", orgtilknytningState);
        var future = orgtilknytningStateKafkaTemplate.send("org.nom.intern-orgtilknytning-state", key, orgtilknytningState);
        try {
            var result = future.get(1, TimeUnit.SECONDS);
            log.info("Sent message: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Failed to send message", e);
        }
        return null;
    }

    @Autowired
    KafkaTemplate<String, OrgtilknytningState> orgtilknytningStateKafkaTemplate;
}
