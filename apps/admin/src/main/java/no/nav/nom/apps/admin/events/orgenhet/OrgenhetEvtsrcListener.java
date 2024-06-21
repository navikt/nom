package no.nav.nom.apps.admin.events.orgenhet;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
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
public class OrgenhetEvtsrcListener {
    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private final OrgenhetEvtsrcRepository orgenhetEvtsrcRepository;
    private final String listenerId;

    public OrgenhetEvtsrcListener(OrgenhetEvtsrcRepository orgenhetEvtsrcRepository, @Value("${app.kafka.orgenhet.eventsource-listener-id}") String listenerId) {
        this.orgenhetEvtsrcRepository = orgenhetEvtsrcRepository;
        this.listenerId = listenerId;
    }

    @KafkaListener(
        containerFactory = "orgenhetEvtsrcKafkaListenerContainerFactory",
        id = "${app.kafka.orgenhet.eventsource-listener-id}",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.orgenhet.eventsourceTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, OrgEnhetEvent> records) {
        orgenhetEvtsrcRepository.addOrgEnhetEvents(StreamSupport.stream(records.spliterator(), false).map(ConsumerRecord::value).toList());
    }

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup.get()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            log.info(
                "All Orgenhet SourceEvents has now been read on startup. Number of OrgenhetEvtsrc messages consumed: {}", orgenhetEvtsrcRepository.count()
            );
            hasReadToEndOnStartup.set(true);
        }
    }
}
