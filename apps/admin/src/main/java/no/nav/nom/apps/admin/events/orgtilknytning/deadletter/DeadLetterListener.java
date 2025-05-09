package no.nav.nom.apps.admin.events.orgtilknytning.deadletter;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.admin.dto.deadletter.DeadLetterDTO;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class DeadLetterListener {

    private final DeadLetterRepository repository;
    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private final String listenerId;

    public DeadLetterListener(DeadLetterRepository repository, String deadLetterUniqueListenerId) {
        this.repository = repository;
        this.listenerId = deadLetterUniqueListenerId;
    }

    @KafkaListener(
        containerFactory = "deadLetterKafkaListenerContainerFactory",
        id = "#{deadLetterUniqueListenerId}",
        topicPartitions = {
            @TopicPartition(
                topic = "${app.kafka.dead-letter.orgtilknytning-topic}",
                partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
            )
        }
    )
    public void onMessage(ConsumerRecords<String, String> records) {
        log.info("Orgtilknytning dead letter: {}", records.count());
        StreamSupport.stream(records.spliterator(), false)
            .map(stringStringConsumerRecord ->
                new DeadLetterDTO(
                    stringStringConsumerRecord.key(),
                    stringStringConsumerRecord.headers().toString(),
                    stringStringConsumerRecord.value()
                ))
            .forEach(this::saveMessage);
    }

    private void saveMessage(DeadLetterDTO deadLetterDTO) {
        repository.addDeadLetter(deadLetterDTO);
    }

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup.get()) {
            return;
        }

        log.debug("Idle event received for listenerId: {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().equals(listenerId)) {
            log.info("DeadLetterListener has read to end on startup");
            hasReadToEndOnStartup.set(true);
        }
    }
}
