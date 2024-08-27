package no.nav.nom.apps.admin.events.orgtilknytning.api;


import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.admin.dto.orgtilknytning.OrgtilknytningStateDTO;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class OrgtilknytningStateApiListner {
    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private final OrgtilknytningStateApiRepository orgtilknytningStateApiRepository;
    private final String listenerId;

    public OrgtilknytningStateApiListner(OrgtilknytningStateApiRepository orgtilknytningStateApiRepository, @Value("${app.kafka.orgtilknytning.apiEventTopic-listener-id}") String listenerId) {
        this.orgtilknytningStateApiRepository = orgtilknytningStateApiRepository;
        this.listenerId = listenerId;
    }

    @KafkaListener(
        containerFactory = "genericApiEventKafkaListenerContainerFactory",
        id = "${app.kafka.orgtilknytning.apiEventTopic-listener-id}",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.orgtilknytning.apiEventTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, OrgtilknytningStateDTO> records) {
        List<OrgtilknytningStateDTO> orgtilknytningTopic = new ArrayList<>();
        records.forEach(record -> orgtilknytningTopic.add(
            OrgtilknytningStateDTO.builder()
                .key(record.key())
                .timestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), ZoneId.systemDefault()))
                .fid(record.value().fid())
                .navident(record.value().navident())
                .orgEnhetId(record.value().orgEnhetId())
                .erDagligOppfolging(record.value().erDagligOppfolging())
                .gyldigFom(record.value().gyldigFom())
                .gyldigTom(record.value().gyldigTom())
                .orgtilknytningType(record.value().orgtilknytningType())
                .build()));
        orgtilknytningStateApiRepository.addOrgtilknytningApiStateEvents(orgtilknytningTopic);
    }

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup.get()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            log.info(
                "All OrgtilknytningState events has now been read on startup. Number of OrgtilknytningApiState messages consumed: {}", orgtilknytningStateApiRepository.count()
            );
            hasReadToEndOnStartup.set(true);
        }
    }
}
