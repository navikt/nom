package no.nav.nom.apps.admin.events.hendelser;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.admin.dto.hendelse.HendelseDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
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
public class HendelserApiStateListener {
    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private final HendelserApiStateRepository hendelserApiStateRepository;
    private final String listenerId;

    public HendelserApiStateListener(HendelserApiStateRepository hendelserApiStateRepository, @Value("${app.kafka.hendelser.apiEventTopic-listener-id}") String listenerId) {
        this.hendelserApiStateRepository = hendelserApiStateRepository;
        this.listenerId = listenerId;
    }

    @KafkaListener(
        containerFactory = "genericApiEventKafkaListenerContainerFactory",
        id = "${app.kafka.hendelser.apiEventTopic-listener-id}",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.hendelser.apiEventTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, HendelseDTO> records) {
        log.debug("Received {} HendelseApiState events", records);
        records.forEach(record -> {
            HendelseDTO hendelseEvent = record.value();
            log.debug("Received HendelseApiState event with id: {}", hendelseEvent.toString());
        });
        hendelserApiStateRepository.addHendelseApiStateEvents(StreamSupport.stream(records.spliterator(), false).map(ConsumerRecord::value).toList());
    }

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup.get()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            log.info(
                "All HendelseApiState events has now been read on startup. Number of HendelseApiState messages consumed: {}", hendelserApiStateRepository.count()
            );
            hasReadToEndOnStartup.set(true);
        }
    }
}
