package no.nav.nom.apps.admin.events.ressurshendelser;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.admin.dto.ressurs.RessursHendelseKafkaDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
public class RessursHendelseApiListener {
    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private final RessursHendelseApiRepository ressursHendelseApiRepository;
    private final String listenerId;


    public RessursHendelseApiListener(
        RessursHendelseApiRepository ressursHendelseApiRepository,
        String apiHendelseRessursUniqueListenerId
    ) {
        this.ressursHendelseApiRepository = ressursHendelseApiRepository;
        this.listenerId = apiHendelseRessursUniqueListenerId;
    }

    @KafkaListener(
        containerFactory = "genericApiEventKafkaListenerContainerFactory",
        id = "#{apiHendelseRessursUniqueListenerId}",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.ressurs-hendelser.apiEventTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, RessursHendelseKafkaDTO> records) {
        log.debug("Received {} RessursHendelseApi events", records);
        records.forEach(record -> {
            RessursHendelseKafkaDTO hendelseEvent = record.value();
            log.debug("Received RessursHendelseApi event with id: {}", hendelseEvent.toString());
        });
        ressursHendelseApiRepository.addRessursHendelseApiEvents(
            StreamSupport.stream(records.spliterator(), false).map(ConsumerRecord::value).toList());
    }

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup.get()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            log.info(
                "All RessursHendelseApi events has now been read on startup. Number of RessursHendelseApi messages consumed: {}",
                ressursHendelseApiRepository.count()
            );
            hasReadToEndOnStartup.set(true);
        }
    }
}
