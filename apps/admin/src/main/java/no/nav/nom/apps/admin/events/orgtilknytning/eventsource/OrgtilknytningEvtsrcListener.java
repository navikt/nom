package no.nav.nom.apps.admin.events.orgtilknytning.eventsource;

import lombok.extern.slf4j.Slf4j;

import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
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
public class OrgtilknytningEvtsrcListener {

    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private final OrgtilknytningEvtsrcRepository orgtilknytningEvtsrcRepository;
    private final String listenerId;

    public OrgtilknytningEvtsrcListener(OrgtilknytningEvtsrcRepository orgtilknytningEvtsrcRepository, @Value("${app.kafka.orgtilknytning.eventsource-listener-id}") String listenerId) {
        this.orgtilknytningEvtsrcRepository = orgtilknytningEvtsrcRepository;
        this.listenerId = listenerId;
    }

    @KafkaListener(
        containerFactory = "orgtilknytningEvtsrcKafkaListenerContainerFactory",
        id = "${app.kafka.orgtilknytning.eventsource-listener-id}",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.orgtilknytning.eventsourceTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, OrgtilknytningEvent> records) {
        orgtilknytningEvtsrcRepository.addOrgtilknytningEvents(StreamSupport.stream(records.spliterator(), false).map(ConsumerRecord::value).toList());
    }

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup.get()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            log.info(
                "All Orgtilknytning SourceEvents has now been read on startup. Number of OrgtilknytningEvtsrc messages consumed: {}", orgtilknytningEvtsrcRepository.count()
            );
            hasReadToEndOnStartup.set(true);
        }
    }
}
