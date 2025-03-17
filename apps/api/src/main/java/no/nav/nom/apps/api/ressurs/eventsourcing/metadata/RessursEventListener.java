package no.nav.nom.apps.api.ressurs.eventsourcing.metadata;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
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
    private final RessursStateRepository ressursStateRepository;

    public RessursEventListener(
        @Value("${app.kafka.ressurs.eventsourceTopic}") String eventSourceTopic,
        RessursMetadataRepository ressursMetadataRepository,
        RessursStateRepository ressursStateRepository
    ) {
        this.ressursMetadataRepository = ressursMetadataRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.eventSourceTopic = eventSourceTopic;
    }

    @KafkaListener(
        id = "#{ressursEventUniqueListenerId}",
        containerFactory = "ressursEventListenerContainerFactory",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.ressurs.eventsourceTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, RessursEvent> records, Consumer<String, String> consumer) {
        StreamSupport.stream(records.spliterator(), false).forEachOrdered(this::process);

        KafkaUtils.verifyUpToDate(consumer, eventSourceTopic, initialLoadComplete,
            () -> log.info(
                "Kafka RessursEvent listener initial load finished. Metadata for {} ressurser retrieved",
                ressursMetadataRepository.countOpprettelseMetadata()
            )
        );
    }

    private void process(ConsumerRecord<String, RessursEvent> record) {
        if (RessursOpprettetEventV1.class.isInstance(record.value())) {
            var key = ((RessursOpprettetEventV1) record.value()).getRessursOpprettetAdditionalData().navident().get();

            ressursMetadataRepository.saveOpprettelseMetadata(key, record.value().getMetadata());
        }

        ressursStateRepository.findByFid(record.value().getFid()).ifPresent(
            ressurs -> ressursMetadataRepository.saveEndringMetadata(
                ressurs.navident(),
                record.value().getMetadata(),
                record.value().getClass().getSimpleName()
            )
        );
    }
}
