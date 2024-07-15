package no.nav.nom.apps.admin.events.orgenhet.api;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.admin.dto.orgenhet.OrgEnhetStateDTO;
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
public class OrgEnhetStateApiListener {
    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private final OrgEnhetStateApiRepository orgEnhetStateApiRepository;
    private final String listenerId;

    public OrgEnhetStateApiListener(OrgEnhetStateApiRepository orgEnhetStateApiRepository, @Value("${app.kafka.orgenhet.apiEventTopic-listener-id}") String listenerId) {
        this.orgEnhetStateApiRepository = orgEnhetStateApiRepository;
        this.listenerId = listenerId;
    }

    @KafkaListener(
        containerFactory = "genericApiEventKafkaListenerContainerFactory",
        id = "${app.kafka.orgenhet.apiEventTopic-listener-id}",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.orgenhet.apiEventTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, OrgEnhetStateDTO> records) {
        List<OrgEnhetStateDTO> orgEnhetTopic = new ArrayList<>();
        records.forEach(record -> orgEnhetTopic.add(
            OrgEnhetStateDTO.builder()
                .key(record.key())
                .timestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), ZoneId.systemDefault()))
                .id(record.value().id())
                .navn(record.value().navn())
                .gyldigFom(record.value().gyldigFom())
                .gyldigTom(record.value().gyldigTom())
                .nomNivaa(record.value().nomNivaa())
                .orgEnhetsType(record.value().orgEnhetsType())
            .build()));
        orgEnhetStateApiRepository.addOrgtilknytningApiStateEvents(orgEnhetTopic);
    }

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup.get()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            log.info(
                "All OrgEnhetState events has now been read on startup. Number of OrgEnhetApiState messages consumed: {}", orgEnhetStateApiRepository.count()
            );
            hasReadToEndOnStartup.set(true);
        }
    }
}
