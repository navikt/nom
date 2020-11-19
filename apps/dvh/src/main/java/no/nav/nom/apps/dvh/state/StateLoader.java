package no.nav.nom.apps.dvh.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.dvh.common.AppProperties;
import no.nav.nom.contract.dvh.DvhEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

@Slf4j
@Component
@RequiredArgsConstructor
class StateLoader {
    private final ConsumerFactory<String, String> consumerFactory;
    private final AppProperties properties;
    private final ObjectMapper mapper;

    @Timed("dvh.state.loader")
    public Map<String, Long> loadState() {
        val topicPartition = new TopicPartition(properties.getDvh().getTopic(), 0); // Only 1 partition
        val recordCounter = new AtomicLong();
        val result = new HashMap<String, Long>();

        try (val consumer = consumerFactory.createConsumer("-state")) {
            consumer.assign(List.of(topicPartition));
            consumer.seekToBeginning(List.of(topicPartition));
            var endOffset = findEndOffsetForPartition(consumer, topicPartition);

            while (!isComplete(topicPartition, consumer, endOffset)) {
                val currentMaxState = findMaxStateByKeyInBatch(consumer, recordCounter);
                result.putAll(currentMaxState);
            }
        }

        val logState = result.entrySet().stream().map(e -> "[Key: " + e.getKey() + " Value: " + e.getValue() + "]").collect(joining(", "));
        log.info("State loaded from Kafka. State: {}, total records read: {}", logState, recordCounter.get());
        return result;
    }

    private long findEndOffsetForPartition(Consumer<String, String> consumer, TopicPartition partition) {
        return consumer.endOffsets(List.of(partition)).entrySet().iterator().next().getValue();
    }

    private boolean isComplete(TopicPartition topicPartition, Consumer<String, String> stateConsumer, long endOffset) {
        val position = stateConsumer.position(topicPartition);
        return position == endOffset;
    }

    private Map<String, Long> findMaxStateByKeyInBatch(Consumer<String, String> consumer, AtomicLong recordCounter) {
        val records = consumer.poll(Duration.ofHours(1));
        val result = new HashMap<String, Long>();
        StreamSupport.stream(records.spliterator(), false)
                .peek(e -> recordCounter.incrementAndGet())
                .map(this::toDvhEvent)
                .collect(groupingBy(DvhEvent::getType))
                .values()
                .forEach(e -> e.stream()
                        .max((o1, o2) -> (int) (o1.getOffset() - o2.getOffset()))
                        .ifPresent(dvh -> result.put(dvh.getType(), dvh.getOffset())));
        return result;
    }

    @SneakyThrows
    private DvhEvent toDvhEvent(ConsumerRecord<String, String> record) {
        return mapper.readValue(record.value(), DvhEvent.class);
    }
}
