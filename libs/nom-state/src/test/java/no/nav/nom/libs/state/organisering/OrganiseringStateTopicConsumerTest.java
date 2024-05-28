package no.nav.nom.libs.state.organisering;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrganiseringState;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(
    classes = {
        NomStateOrganiseringListener.class
        , PropertiesAutoConfig.class
        , OrganiseringStateAutoconfiguration.class
        , KafkaProperties.class
        , ObjectMapper.class
    }
)
@Import(KafkaTestFactoryConfiguration.class)
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {"org.nom.intern-organisering-state"}, controlledShutdown = false)
@EnableKafka
public class OrganiseringStateTopicConsumerTest {

    private CountDownLatch alleMeldingerLest = new CountDownLatch(1);

    @Autowired
    NomStateOrganiseringListener nomStateOrganiseringListener;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("Test that we can consume messages from org.nom.intern-organisering-state")
    public void testConsumeMessages() throws InterruptedException {
        sendOrganiseringState(
            "FID_1",
            OrganiseringState.builder()
                .fid("FID_1")
                .id("ID_2")
                .idOver("ID_1")
                .navn("NAVN_1")
                .navnOver("NAVN_2")
                .gyldigFom(LocalDate.of(2021, 1, 1))
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
            nomStateOrganiseringListener.register(msgVisitor);
            registeredVisitor = true;
        }
        resetLatch();
    }

    AbstractNomStateListener.Visitor<OrganiseringState> msgVisitor = new NomStateOrganiseringListener.Visitor<OrganiseringState>() {
        @Override
        public String visitorName() {
            return "TestVisitor";
        }

        @Override
        public void visit(String fid, OrganiseringState organiseringState) {
            log.info("Visitor received message: {}-{}", fid, organiseringState);
            alleMeldingerLest.countDown();
        }
    };

    public void resetLatch() {
        alleMeldingerLest = new CountDownLatch(1);
    }

    private SendResult<String, OrganiseringState> sendOrganiseringState(String key, OrganiseringState organiseringState) {
        log.info("Sending message: {}", organiseringState);
        var future = organiseringStateKafkaTemplate.send("org.nom.intern-organisering-state", key, organiseringState);
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
    KafkaTemplate<String, OrganiseringState> organiseringStateKafkaTemplate;
}
