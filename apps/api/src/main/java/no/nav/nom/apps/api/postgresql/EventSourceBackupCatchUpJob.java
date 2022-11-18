package no.nav.nom.apps.api.postgresql;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.Event;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EventSourceBackupCatchUpJob implements ConsumerSeekAware {
    private final long START_BACKUP_CHECKER_INTERVAL = 1000*60*2;

    private final EventSourceRepository eventSourceRepository;
    private final ObjectMapper objectMapper;
    private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    private Optional<String> firstBackedUpKey = Optional.empty();
    private int numBackedUp;

    public EventSourceBackupCatchUpJob(EventSourceRepository eventSourceRepository, ObjectMapper objectMapper, KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry) {
        this.eventSourceRepository = eventSourceRepository;
        this.objectMapper = objectMapper;
        this.kafkaListenerEndpointRegistry = kafkaListenerEndpointRegistry;
    }

    @KafkaListener(
            id = "EventSourceBackupCatchUpJob",
            topics = "${app.kafka.eventsourceTopic}",
            containerFactory = "eventsourceKafkaListenerContainerFactory",
            autoStartup = "false"
    )
    public void listen(ConsumerRecord<String, Event> record) {
        if (record.key().compareTo(firstBackedUpKey.get()) >= 0) { // We're all caught up
            stopConsumingSourceEvents();
        }
        eventSourceRepository.save(
                EventSource.builder()
                        .eventKey(record.key())
                        .payload(objectMapper.valueToTree(record.value()))
                        .build()
        );
        numBackedUp++;
    }

    // Do not start to consume eventsource until normal eventsource backup to repo has started
    // Then this routine will process all, if any, events produced before normal eventsource backup was implemented
    @Scheduled(fixedDelay = START_BACKUP_CHECKER_INTERVAL)
    public void checkToStartBackingUp(){
        if (firstBackedUpKey.isPresent()) { // Already visited
            return;
        }
        firstBackedUpKey = eventSourceRepository.findFirstSavedSourceEvent();
        if (firstBackedUpKey.isPresent()) {
            startConsumingSourceEvents();
        }
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        final List<String> topics = assignments.keySet().stream().map(p -> p.topic() ).collect(Collectors.toList());
        log.info("Kafka partitions on topics %s assigned to listener %s. Seek to beginning in order to backup old messages".formatted(topics, EventsourceListenerConfig.GROUP_ID));
        callback.seekToBeginning(assignments.keySet());
    }

    private void startConsumingSourceEvents() {
        log.info("Starting to consume source events for recovery backup. End at event key {}", firstBackedUpKey.get());
        kafkaListenerEndpointRegistry.getListenerContainer(EventsourceListenerConfig.GROUP_ID).start();
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        log.info("Done consuming source events for recovery backup. {} events backed up.", numBackedUp);
    }

    private void stopConsumingSourceEvents() {
        kafkaListenerEndpointRegistry.getListenerContainer(EventsourceListenerConfig.GROUP_ID).stop();
    }
}
