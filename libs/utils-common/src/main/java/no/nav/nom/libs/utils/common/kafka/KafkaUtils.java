package no.nav.nom.libs.utils.common.kafka;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class KafkaUtils {

    public static void assignRandomTransactionIdPrefix() {
        System.setProperty("spring.kafka.producer.transaction-id-prefix", UUID.randomUUID().toString().substring(0, 8));
    }

    public static void verifyUpToDate(Consumer<String, String> consumer, String topic, AtomicBoolean initialLoadComplete) {
        verifyUpToDate(consumer, topic, initialLoadComplete, null);
    }

    public static void verifyUpToDate(Consumer<String, String> consumer, String topic, AtomicBoolean initialLoadComplete, Runnable callback) {
        if (!initialLoadComplete.get()) {
            val endOffset = findEndOffsetForPartition(consumer, topic, 0);
            val complete = isAtOrAfterOffsetForPartition(consumer, topic, 0, endOffset);

            if (endOffset == 0 || complete) {
                initialLoadComplete.set(true);
                if (callback != null) {
                    callback.run();
                }
            }
        }
    }

    public static long findEndOffsetForPartition(Consumer<?, ?> consumer, String topic, int partition) {
        return consumer.endOffsets(List.of(new TopicPartition(topic, partition))).entrySet().iterator().next().getValue();
    }

    private static boolean isAtOrAfterOffsetForPartition(Consumer<?, ?> stateConsumer, String topic, int partition, long offset) {
        val position = stateConsumer.position(new TopicPartition(topic, partition));
        return position >= offset;
    }

}
