package no.nav.nom.apps.admin.events.ressurs.eventsource;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
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
public class RessursEvtsrcListener {
    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private final RessursEvtsrcRepository ressursEvtsrcRepository;
    private final String listenerId;

    public RessursEvtsrcListener(RessursEvtsrcRepository ressursEvtsrcRepository, String evtsrcRessursUniqueListenerId) {
        this.ressursEvtsrcRepository = ressursEvtsrcRepository;
        this.listenerId = evtsrcRessursUniqueListenerId;
    }

    @KafkaListener(
        containerFactory = "ressursEvtsrcKafkaListenerContainerFactory",
        id = "#{evtsrcRessursUniqueListenerId}",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.ressurs.eventsourceTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, RessursEvent> records) {
        ressursEvtsrcRepository.addRessursEvents(StreamSupport.stream(records.spliterator(), false).map(ConsumerRecord::value).toList());
    }

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup.get()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            log.info(
                "All Ressurs SourceEvents has now been read on startup. Number of RessursEvtsrc messages consumed: {}", ressursEvtsrcRepository.count()
            );
            hasReadToEndOnStartup.set(true);
        }
    }
}
