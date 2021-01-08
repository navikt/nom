package no.nav.nom.apps.dvh;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.dvh.common.AppProperties;
import no.nav.nom.apps.dvh.state.StateRepository;
import no.nav.nom.contract.dvh.DvhEvent;
import no.nav.nom.contract.dvh.Enhet;
import no.nav.nom.contract.dvh.Ressurs;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles({"test", "log-console-error"})
@AutoConfigureWireMock(port = 8081)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class TestBase {

    @SpyBean
    protected StateRepository stateRepository;

    @MockBean
    protected KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    protected AppProperties properties;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void reset() {
        Mockito.reset(stateRepository, kafkaTemplate);
        when(kafkaTemplate.executeInTransaction(any())).then(mi -> {
            val arg = (KafkaOperations.OperationsCallback) mi.getArgument(0);
            return arg.doInOperations(kafkaTemplate);
        });
    }

    @SuppressWarnings("unchecked")
    public List<ProducerRecord<String, String>> getRecordsFromKafka() {
        val captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate, atLeastOnce()).send(captor.capture());
        return captor.getAllValues().stream()
                .map(e -> (ProducerRecord<String, String>) e)
                .collect(toUnmodifiableList());
    }

    public void assertBasicRecord(ProducerRecord record, String topic, String key) {
        assertEquals(key, record.key());
        assertNull(record.partition());
        assertEquals(topic, record.topic());
        assertNull(record.timestamp());
        assertNotEquals(null, record.value());
    }

    @SneakyThrows
    public DvhEvent toDvhEvent(ProducerRecord<String, String> record) {
        return objectMapper.readValue(record.value(), DvhEvent.class);
    }

    public void assertBasicDvhEvent(ProducerRecord<String, String> record, String eventType) {
        val asDvhEvent = toDvhEvent(record);
        UUID.fromString(asDvhEvent.getId()); // Check if valid UUID
        assertEquals(eventType, asDvhEvent.getType());
        if (eventType.equals(Enhet.TYPE)) {
            assertEquals("Enhet-" + asDvhEvent.getOffset(), record.key());
        } else if (eventType.equals(Ressurs.TYPE)) {
            assertEquals("Ressurs-" + asDvhEvent.getOffset(), record.key());
        }
    }
}
