package no.nav.nom.libs.state;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import no.nav.nom.libs.state.common.PropertiesAutoConfig;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@SpringBootTest(classes = {
        PropertiesAutoConfig.class
        , KafkaProperties.class
        , JsonMapper.class
})
@DirtiesContext
@EmbeddedKafka(partitions = 1)
@EnableKafka
public abstract class StateTopicConsumerTestBase<T> {
    protected CountDownLatch alleMeldingerLest;
    private boolean registeredVisitor = false;

    protected String stateTopic;
    protected T testMessage;
    protected final AbstractNomStateListener<T> stateListenerUnderTest;

    @Value("${spring.embedded.kafka.brokers}")
    String kafkabroker;
    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapservers;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    KafkaTemplate<String, T> kafkaTemplate;

    @Autowired
    KafkaTemplate<String, String> stringProducerTemplate;

    protected StateTopicConsumerTestBase(AbstractNomStateListener<T> stateListenerUnderTest) {
        this.stateListenerUnderTest = stateListenerUnderTest;
    }

    @BeforeEach
    public void registerVisitor() {
        log.info("Kafka broker:      {}", kafkabroker);
        log.info("Bootstrap-servers: {}", bootstrapservers);
        if (!registeredVisitor) {
            stateListenerUnderTest.register(failingVisitor); // register before completedReadMsgVisitor in order to be able to complete in time
            stateListenerUnderTest.register(completedReadMsgVisitor);
            registeredVisitor = true;
        }
        stateListenerUnderTest.resetStateConsumption();
    }

    @Test
    @DisplayName("Test consumption of illegal Json on startup")
    public void testConsumeIllegalJson() throws InterruptedException {
        setMessageCount(2);

        sendMessage("FID_1", testMessage);
        sendIllegalJson("FAIL_FID", " !! BOGUS JSON !! ");
        sendMessage("FID_2", testMessage);

        alleMeldingerLest.await();

        assertFalse(stateListenerUnderTest.hasReadToEndOnStartup());
        assertTrue(stateListenerUnderTest.isAbortedOnStartup());
    }

    @Test
    @DisplayName("Test failed visitor on startup")
    public void testConsumeFailedVisitor() throws InterruptedException {
        stateListenerUnderTest.register(failingVisitor);
        setMessageCount(3);

        sendMessage("FID_1", testMessage);
        sendMessage("FAIL_FID", testMessage);
        sendMessage("FID_2", testMessage);

        alleMeldingerLest.await();

        assertFalse(stateListenerUnderTest.hasReadToEndOnStartup());
        assertTrue(stateListenerUnderTest.isAbortedOnStartup());
    }

    @Test
    @DisplayName("Test consumption of illegal Json after startup")
    public void testConsumeIllegalJsonAfterStartup() throws InterruptedException {
        stateListenerUnderTest.setHasReadToEndOnStartup();
        setMessageCount(2);

        sendMessage("FID_1", testMessage);
        sendIllegalJson("FAIL_FID", " !! BOGUS JSON !! ");
        sendMessage("FID_2", testMessage);

        alleMeldingerLest.await();

        assertTrue(stateListenerUnderTest.hasReadToEndOnStartup());
        assertFalse(stateListenerUnderTest.isAbortedOnStartup());
    }

    @Test
    @DisplayName("Test failed visitor after startup")
    public void testConsumeFailedVisitorAfterStartup() throws InterruptedException {
        stateListenerUnderTest.setHasReadToEndOnStartup();
        setMessageCount(3);

        sendMessage("FID_1", testMessage);
        sendMessage("FAIL_FID", testMessage);
        sendMessage("FID_2", testMessage);

        alleMeldingerLest.await();

        assertTrue(stateListenerUnderTest.hasReadToEndOnStartup());
        assertFalse(stateListenerUnderTest.isAbortedOnStartup());
    }

    AbstractNomStateListener.Visitor<T> completedReadMsgVisitor = new NomStateOrgEnhetListener.Visitor<>() {
        @Override
        public String visitorName() {
            return "TestVisitor";
        }

        @Override
        public void visit(String fid, T state) {
            log.info("Visitor received message: {}-{}", fid, state);
            alleMeldingerLest.countDown();
        }
    };

    protected AbstractNomStateListener.Visitor<T> failingVisitor = new NomStateOrgEnhetListener.Visitor<>() {
        @Override
        public String visitorName() {
            return "FailingVisitor";
        }

        @Override
        public void visit(String fid, T state) {
            if (fid.equals("FAIL_FID")) {
                throw new RuntimeException("Failing visitor");
            }
        }
    };

    protected void setMessageCount(int count) { alleMeldingerLest = new CountDownLatch(count); }

    protected SendResult<String, T> sendMessage(String key, T state) {
        log.info("Sending message: {}", state);
        var future = kafkaTemplate.send(stateTopic, key, state);
        try {
            var result = future.get(1, TimeUnit.SECONDS);
            log.info("Sent message: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Failed to send message", e);
        }
        return null;
    }

    protected SendResult<String, String> sendIllegalJson(String key, String illegalJson) {
        log.info("Sending illegal message: {}", illegalJson);
        var future = stringProducerTemplate.send(stateTopic, key, illegalJson);
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
