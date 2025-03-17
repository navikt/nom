package no.nav.nom.apps.admin.events.tilgang;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
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
public class TilgangEvtsrcListener {
    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private final TilgangEvtsrcRepository tilgangEvtsrcRepository;
    private final String listenerId;

    public TilgangEvtsrcListener(TilgangEvtsrcRepository tilgangEvtsrcRepository, String evtsrcTilgangUniqueListenerId) {
        this.tilgangEvtsrcRepository = tilgangEvtsrcRepository;
        this.listenerId = evtsrcTilgangUniqueListenerId;
    }

    @KafkaListener(
        containerFactory = "nomStateDelegertLederKafkaListenerContainerFactory",
        id = "#{evtsrcTilgangUniqueListenerId}",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.authorize.eventsourceTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, AuthorizationEvent> records) {
        tilgangEvtsrcRepository.addTilgangEvents(StreamSupport.stream(records.spliterator(), false).map(ConsumerRecord::value).toList());
    }

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup.get()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            log.info(
                "All Tilgang SourceEvents has now been read on startup. Number of TilgangEvtsrc messages consumed: {}", tilgangEvtsrcRepository.count()
            );
            hasReadToEndOnStartup.set(true);
        }
    }
}
