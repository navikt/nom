package no.nav.nom.libs.state.ressurs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(
    classes = {
        NomStateRessursListener.class
        , PropertiesAutoConfig.class
        , RessursStateAutoconfiguration.class
        , KafkaProperties.class
        , ObjectMapper.class
    }
)
@Import(KafkaTestFactoryConfiguration.class)
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {"org.nom.intern-ressurs-state"})
@EnableKafka
public class RessursStateTopicConsumerTest {

    private CountDownLatch alleMeldingerLest = new CountDownLatch(1);

    @Autowired
    NomStateRessursListener nomStateRessursListener;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    KafkaTemplate<String, RessursState> ressursStateKafkaTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("Test that we can consume messages from org.nom.intern-ressurs-state")
    public void testConsumeMessages() throws InterruptedException {
        sendRessursState(
            "FID_1",
            RessursState.builder()
                .fid("FID_1")
                .navident("A000001")
                .personident("12345678901")
                .visningsnavn(new Visningsnavn("John", Optional.empty(), "Doe"))
                .startdato(Optional.of(LocalDate.of(2022, 1, 1)))
                .sektor(Sektor.NAV_STATLIG)
                .build()
        );

        sendRessursState(
            "FID_2",
            RessursState.builder()
                .fid("FID_2")
                .navident("A000002")
                .personident("12345678902")
                .visningsnavn(new Visningsnavn("Jane", Optional.empty(), "Doe"))
                .startdato(Optional.of(LocalDate.of(2022, 1, 1)))
                .sektor(Sektor.NAV_STATLIG)
                .primaryFysiskBrukersted(Optional.of("1234"))
                .fysiskeBrukersteder(List.of("1234", "5678"))
                .primaryTelefonnummer(Optional.of("12345678"))
                .build()
        );

        alleMeldingerLest.await();

        Awaitility.await().until(() -> nomStateRessursListener.hasReadToEndOnStartup());
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
            nomStateRessursListener.register(msgVisitor);
            registeredVisitor = true;
        }
        resetLatch();
    }

    NomStateRessursListener.Visitor msgVisitor = new NomStateRessursListener.Visitor<RessursState>() {
        @Override
        public String visitorName() {
            return "TestVisitor";
        }

        @Override
        public void visit(RessursState ressursState) {
            log.info("Visitor received message: {}", ressursState);
            alleMeldingerLest.countDown();
        }
    };

    public void resetLatch() {
        alleMeldingerLest = new CountDownLatch(1);
    }

    private SendResult<String, RessursState> sendRessursState(String key, RessursState ressursState) {
        log.info("Sending message: {}", ressursState);
        var future = ressursStateKafkaTemplate.send("org.nom.intern-ressurs-state", key, ressursState);
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
