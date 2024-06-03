package no.nav.nom.libs.state.orgenhet;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgEnhetState;
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
        NomStateOrgEnhetListener.class
        , PropertiesAutoConfig.class
        , OrgEnhetStateAutoconfiguration.class
        , KafkaProperties.class
        , ObjectMapper.class
    }
)
@Import(KafkaTestFactoryConfiguration.class)
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {"org.nom.intern-orgenhet-state"}, controlledShutdown = false)
@EnableKafka
public class OrgEnhetStateTopicConsumerTest {

    private CountDownLatch alleMeldingerLest;

    @Autowired
    NomStateOrgEnhetListener nomStateOrgEnhetListener;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("Test that we can consume messages from org.nom.intern-orgenhet-state")
    public void testConsumeMessages() throws InterruptedException {
        setMessageCount(1);

        sendOrgEnhetState(
            "FID_1",
            OrgEnhetState.builder()
                .fid("FID_1")
                .id("ID_1")
                .agressoId(Optional.of("001"))
                .agressoOrgNivaa(Optional.of("ORGENHET"))
                .navn("NAVNET")
                .gyldigFom(LocalDate.of(2021, 1, 1))
                .uniqueAgressoId(Optional.of("001-NAVNET-ORGENHET"))
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
            nomStateOrgEnhetListener.register(msgVisitor);
            registeredVisitor = true;
        }
    }

    AbstractNomStateListener.Visitor<OrgEnhetState> msgVisitor = new NomStateOrgEnhetListener.Visitor<OrgEnhetState>() {
        @Override
        public String visitorName() {
            return "TestVisitor";
        }

        @Override
        public void visit(String fid, OrgEnhetState orgEnhetState) {
            log.info("Visitor received message: {}-{}", fid, orgEnhetState);
            alleMeldingerLest.countDown();
        }
    };

    public void setMessageCount(int count) { alleMeldingerLest = new CountDownLatch(count); }

    private SendResult<String, OrgEnhetState> sendOrgEnhetState(String key, OrgEnhetState orgEnhetState) {
        log.info("Sending message: {}", orgEnhetState);
        var future = orgEnhetStateKafkaTemplate.send("org.nom.intern-orgenhet-state", key, orgEnhetState);
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
    KafkaTemplate<String, OrgEnhetState> orgEnhetStateKafkaTemplate;
}
