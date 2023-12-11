package no.nav.nom.apps.api.ressurs.eventsourcing.metadata;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.libs.utils.common.kafka.KafkaUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;


@Slf4j
@Component
public class RessursEventListener {

    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final String eventSourceTopic;
    private final RessursMetadataRepository ressursMetadataRepository;

    public RessursEventListener(
        @Value("${app.kafka.ressurs.eventsourceTopic}") String eventSourceTopic,
        RessursMetadataRepository ressursMetadataRepository
    ) {
        this.ressursMetadataRepository = ressursMetadataRepository;
        this.eventSourceTopic = eventSourceTopic;
    }

    @KafkaListener(
        containerFactory = "ressursEventListenerContainerFactory",
        clientIdPrefix = "ressursEventListener",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.ressurs.eventsourceTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, RessursEvent> records, Consumer<String, String> consumer) {
        StreamSupport.stream(records.spliterator(), false).forEachOrdered(this::process);

        KafkaUtils.verifyUpToDate(consumer, eventSourceTopic, initialLoadComplete,
            () -> log.info("Kafka RessursEvent listener initial load finished. Metadata for {} ressurser retrieved", ressursMetadataRepository.count())
        );
    }

    private void process(ConsumerRecord<String, RessursEvent> record) {
        if (RessursOpprettetEventV1.class.isInstance(record.value())) {
            var key = ((RessursOpprettetEventV1)record.value()).getRessursOpprettetAdditionalData().navident().get();

            ressursMetadataRepository.save(key, record.value().getMetadata());
        }
    }
}
