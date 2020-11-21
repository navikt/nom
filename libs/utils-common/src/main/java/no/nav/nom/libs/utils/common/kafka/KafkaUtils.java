package no.nav.nom.libs.utils.common.kafka;

import lombok.val;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;

import java.util.List;
import java.util.UUID;

public class KafkaUtils {

    public static void assignRandomTransactionIdPrefix() {
        System.setProperty("spring.kafka.producer.transaction-id-prefix", UUID.randomUUID().toString().substring(0, 8));
    }

    public static long findEndOffsetForPartition(Consumer<?, ?> consumer, String topic, int partition) {
        return consumer.endOffsets(List.of(new TopicPartition(topic, partition))).entrySet().iterator().next().getValue();
    }

    public static boolean isAtOrAfterOffsetForPartition(Consumer<?, ?> stateConsumer, String topic, int partition, long offset) {
        val position = stateConsumer.position(new TopicPartition(topic, partition));
        return position >= offset;
    }
}
