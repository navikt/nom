package no.nav.nom.apps.navident.eventsource;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.domain.PersonidType;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles({"test", "log-console"})
@SpringBootTest
@Slf4j
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
class EventSourceServiceTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public KafkaConsumer<String, String> stringValueConsumer(KafkaProperties properties) {
            properties.setClientId("plaintextConsumer");
            properties.getConsumer().setValueDeserializer(org.apache.kafka.common.serialization.StringDeserializer.class);
            return new KafkaConsumer<>(properties.buildConsumerProperties());
        }
    }

    @Autowired
    private KafkaConsumer<String, String> stringValueConsumer;

    @MockBean
    private DataSource dataSource;

    @Value("${test.topic}")
    private String topic;

    @Autowired
    private EventSourceService eventSourceService;

    @Autowired
    private KafkaTemplate<String, SourceEvent> kafkaTemplate;

    @Test
    public void wireing() throws InterruptedException {
        SourceEvent event = getOpprettEvent("A123456");

        kafkaTemplate.send(topic, event.payload.navident, event);

        assertTrue(eventSourceService.latch.await(1000, TimeUnit.MILLISECONDS));

//        stringValueConsumer.subscribe(Arrays.asList(topic));
//
//        final ConsumerRecords<String, String> consumerRecords =
//                stringValueConsumer.poll(Duration.ofMillis(1000));
//        log.info("Messages consumed: {}", consumerRecords.count());
//        consumerRecords.forEach(r -> log.info(r.value()));
//        stringValueConsumer.commitAsync();
//        stringValueConsumer.close();
    }



    private SourceEvent getOpprettEvent(String navid) {
        return SourceEvent.builder()
                .bestiller("Test-bestiller")
                .event(EventType.OPPRETT)
                .eventTS(LocalDateTime.now())
                .dato(LocalDate.now())
                .payload(
                        EventPayload.builder()
                                .identType(IdentType.STANDARD)
                                .navident(navid)
                                .personid("0101199912345")
                                .personidtype(PersonidType.PDL)
                                .build()
                ).build();
    }


}