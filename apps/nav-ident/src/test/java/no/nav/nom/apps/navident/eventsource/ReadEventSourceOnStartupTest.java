package no.nav.nom.apps.navident.eventsource;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.EmbeddedKafkaTestBase;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.domain.Navident;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Slf4j
class ReadEventSourceOnStartupTest extends EmbeddedKafkaTestBase {
    @Autowired private KafkaTemplate<String, SourceEvent> kafkaTemplate;
    @Autowired private Map<String, Navident> navidenter;

    @Autowired private ConsumerFactory<String, SourceEvent> consumerFactory;
    @Autowired private EventSourceService eventSourceService;

    @Value("${test.topic}")  private String topic;

    @MockBean EventSourceOffsetProbe eventSourceOffsetProbe;

    @Test
    @SneakyThrows
    @Disabled //Bug ved å kjøre denne på GHA....
    public void readEventSourceOnStartup() {

        when(eventSourceOffsetProbe.findLastOffsetOnTopic(anyString())).thenAnswer( prepopulate -> {
                    log.debug("Inside mock to prepopulate EventSource queue");
                    populateTopicOnStartup();
                    return (new EventSourceOffsetProbe(consumerFactory)).findLastOffsetOnTopic(topic);
                }
        );

        await().atMost(5, SECONDS).pollInterval(10, MILLISECONDS).until(eventSourceService::hasAllSourceEventsBeenRead);

        assertEquals(10, navidenter.size());
    }

    @SneakyThrows
    private void populateTopicOnStartup() {
        kafkaTemplate.send(topic, "A000000", getOpprettEvent("A000000", "01019900000"));
        kafkaTemplate.send(topic, "A000001", getOpprettEvent("A000001", "01019900001"));
        kafkaTemplate.send(topic, "A000002", getOpprettEvent("A000002", "01019900002"));
        kafkaTemplate.send(topic, "A000003", getOpprettEvent("A000003", "01019900003"));
        kafkaTemplate.send(topic, "A000004", getOpprettEvent("A000004", "01019900004"));
        kafkaTemplate.send(topic, "A000005", getOpprettEvent("A000005", "01019900005"));
        kafkaTemplate.send(topic, "A000006", getOpprettEvent("A000006", "01019900006"));
        kafkaTemplate.send(topic, "A000007", getOpprettEvent("A000007", "01019900007"));
        kafkaTemplate.send(topic, "A000008", getOpprettEvent("A000008", "01019900008"));
        kafkaTemplate.send(topic, "A000009", getOpprettEvent("A000009", "01019900009"));
    }

    private SourceEvent getOpprettEvent(String navid, String personid) {
        return SourceEvent.builder()
                .bestiller("Test-bestiller")
                .event(EventType.OPPRETT)
                .eventTS(LocalDateTime.now())
                .payload(
                        EventPayload.builder()
                                .identType(IdentType.STANDARD)
                                .navident(navid)
                                .personid(personid)
                                .build()
                ).build();}
}
