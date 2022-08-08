package no.nav.nom.apps.navident.eventsource;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

@Component
@AllArgsConstructor
@Slf4j
public class EventSourceOffsetProbe {
    private final ConsumerFactory<String, SourceEvent> consumerFactory;

    public long findLastOffsetOnTopic(String topic) {
        String startupProbeConsumerId = topic + "-StartupProbeConsumer";
        log.debug("Going to create probe consumer with groupId %s".formatted(startupProbeConsumerId));
        try {
            Consumer<String, SourceEvent> consumer = consumerFactory.createConsumer(startupProbeConsumerId, null);
            consumer.subscribe(
                    Collections.singletonList(topic), new ConsumerRebalanceListener() {

                        @Override
                        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                        }

                        @Override
                        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                            consumer.seekToEnd(partitions);
                        }
            });

            consumer.poll(Duration.ofMillis(100)); // Need to give some time for partitions to be assigned

            long lastOffset = consumer.assignment().stream().mapToLong(consumer::position).max().orElse(0) - 1;
            log.info("Max offset for EventSource on startup: %d".formatted(lastOffset));

            consumer.unsubscribe();
            return lastOffset;
        } catch (Exception e) {
            log.warn("Failed to handle startup probe on EventSource topic");
            return -1;
        }
    }
}
