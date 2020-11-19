package no.nav.nom.apps.dvh.state;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.contract.dvh.DvhEvent;
import no.nav.nom.contract.dvh.Enhet;
import no.nav.nom.contract.dvh.Ressurs;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@DisplayName("StateLoader")
class StateLoaderTest extends StateTestBase {
    private static final long END_OFFSET = 12345678L;
    private Consumer<String, String> consumer;

    @BeforeEach
    void setup() {
        consumer = mock(Consumer.class);
        doReturn(consumer).when(consumerFactory).createConsumer(anyString());
    }

    @Test
    @DisplayName("Laster state fra Kafka og leser til slutten av Kafka topic via EndOffset, beholder siste forekomster av høyeste offset innen hver type")
    void scenario11() {
        mockKafkaPositions(10L, 11L, END_OFFSET);

        mockKafkaRecords(
                List.of(
                        createRecordWithValues(Enhet.TYPE, 1337L),
                        createRecordWithValues(Ressurs.TYPE, 999L)
                ),
                List.of(
                        createRecordWithValues(Enhet.TYPE, 1340L),
                        createRecordWithValues(Ressurs.TYPE, 1001L)
                )
        );

        val result = loader.loadState();

        assertThat(result.size(), is(2));
        assertThat(result.get(Enhet.TYPE), is(1340L));
        assertThat(result.get(Ressurs.TYPE), is(1001L));
    }

    @Test
    @DisplayName("Returnerer ingen forekomster når ingen data på Kafka")
    void scenario12() {
        mockKafkaPositions(END_OFFSET);

        val result = loader.loadState();

        assertThat(result.size(), is(0));
    }

    @Test
    @DisplayName("Beholder kun høyeste offset innen type uavhengig av rekkefølge innen batch")
    void scenario13() {
        mockKafkaPositions(10L, END_OFFSET);

        mockKafkaRecords(
                List.of(
                        createRecordWithValues(Enhet.TYPE, 1337L),
                        createRecordWithValues(Enhet.TYPE, 1335L),
                        createRecordWithValues(Ressurs.TYPE, 999L)
                )
        );

        val result = loader.loadState();

        assertThat(result.size(), is(2));
        assertThat(result.get(Enhet.TYPE), is(1337L));
        assertThat(result.get(Ressurs.TYPE), is(999L));
    }

    @SneakyThrows
    private ConsumerRecord<String, String> createRecordWithValues(String type, long offset) {
        val record = mock(ConsumerRecord.class);
        val dvhAsJson = mapper.writeValueAsString(DvhEvent.builder()
                .offset(offset)
                .type(type)
                .build());
        doReturn(dvhAsJson).when(record).value();
        return record;
    }

    private void mockKafkaPositions(long currentPosition, long... followingPositions) {
        val endOffsets = Map.of(new TopicPartition("", 0), END_OFFSET);
        doReturn(endOffsets).when(consumer).endOffsets(any());
        var current = doReturn(currentPosition);
        var endPosFound = currentPosition == END_OFFSET;
        for (val following : followingPositions) {
            current = current.doReturn(following);
            if (!endPosFound) endPosFound = following == END_OFFSET;
        }
        assertTrue(endPosFound, "No occurrence of end offset found, will cause endless loop");
        current.when(consumer).position(any());
    }

    @SafeVarargs
    private void mockKafkaRecords(List<ConsumerRecord<String, String>> next, List<ConsumerRecord<String, String>>... then) {
        var current = doReturn(new ConsumerRecords<>(Map.of(new TopicPartition("", 0), next)));
        for (val e : then) {
            current = current.doReturn(new ConsumerRecords<>(Map.of(new TopicPartition("", 0), e)));
        }
        current.when(consumer).poll(any());
    }

}
