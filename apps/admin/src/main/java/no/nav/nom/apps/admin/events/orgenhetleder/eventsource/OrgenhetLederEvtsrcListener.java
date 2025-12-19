package no.nav.nom.apps.admin.events.orgenhetleder.eventsource;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederEvent;
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
public class OrgenhetLederEvtsrcListener {
    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private final OrgenhetLederEvtsrcRepository orgenhetLederEvtsrcRepository;
    private final String listenerId;

    public OrgenhetLederEvtsrcListener(OrgenhetLederEvtsrcRepository orgenhetLederEvtsrcRepository, String evtsrcOrgenhetLederUniqueListenerId) {
        this.orgenhetLederEvtsrcRepository = orgenhetLederEvtsrcRepository;
        this.listenerId = evtsrcOrgenhetLederUniqueListenerId;
    }

    @KafkaListener(
        containerFactory = "orgenhetLederEvtsrcKafkaListenerContainerFactory",
        id = "#{evtsrcOrgenhetLederUniqueListenerId}",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.orgEnhetLeder.eventsourceTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, OrgEnhetLederEvent> records) {
        orgenhetLederEvtsrcRepository.addOrgEnhetLederEvents(StreamSupport.stream(records.spliterator(), false).map(ConsumerRecord::value).toList());
    }

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup.get()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            log.info(
                "All OrgenhetLeder SourceEvents has now been read on startup. Number of OrgenhetLederEvtsrc messages consumed: {}", orgenhetLederEvtsrcRepository.count()
            );
            hasReadToEndOnStartup.set(true);
        }
    }
}
