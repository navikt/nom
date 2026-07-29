package no.nav.nom.apps.admin.events.organisering.eventsource;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
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
public class OrganiseringEvtsrcListener {

    private final AtomicBoolean hasReadToEndOnStartUp = new AtomicBoolean(false);
    private final OrganiseringEvtsrcRepository organiseringEventRepository;
    private final String listenerId;

    public OrganiseringEvtsrcListener(
        OrganiseringEvtsrcRepository organiseringEvtsrcRepository,
        String evtsrcOrganiseringUniqueListenerId
    ) {
        this.organiseringEventRepository = organiseringEvtsrcRepository;
        this.listenerId = evtsrcOrganiseringUniqueListenerId;
    }

    @KafkaListener(
        containerFactory = "organiseringEvtsrcKafkaListenerContainerFactory",
        id = "#{evtsrcOrganiseringUniqueListenerId}",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.organisering.eventsourceTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, OrganiseringEvent> records) {
        organiseringEventRepository.addOrganiseringEvents(StreamSupport.stream(records.spliterator(), false).map(ConsumerRecord::value).toList());
    }

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartUp.get()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            log.info(
                "All organisering source events has now been read on startup. Number of OrganiseringEvtsrc messages consumed {}", organiseringEventRepository.count()
            );
            hasReadToEndOnStartUp.set(true);
        }
    }
}
