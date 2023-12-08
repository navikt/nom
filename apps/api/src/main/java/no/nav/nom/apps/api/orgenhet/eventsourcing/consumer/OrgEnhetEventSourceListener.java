package no.nav.nom.apps.api.orgenhet.eventsourcing.consumer;

import no.nav.nom.apps.api.orgenhet.OrgEnhetTypeHandler;
import no.nav.nom.apps.api.orgenhet.OrgEnhetNomNivaaHandler;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Service;

import java.util.stream.StreamSupport;

@Service
public class OrgEnhetEventSourceListener {
    private final OrgEnhetTypeHandler orgEnhetTypeHandler;
    private final OrgEnhetNomNivaaHandler orgEnhetNomNivaaHandler;
    private final Logger logger = LoggerFactory.getLogger(OrgEnhetEventSourceListener.class);

    public OrgEnhetEventSourceListener(
        OrgEnhetTypeHandler orgEnhetTypeHandler,
        OrgEnhetNomNivaaHandler orgEnhetNomNivaaHandler
    ) {
        this.orgEnhetTypeHandler = orgEnhetTypeHandler;
        this.orgEnhetNomNivaaHandler = orgEnhetNomNivaaHandler;
    }

    @KafkaListener(
        id = "nomApi_TypeAndNivaaHandler",
        containerFactory = "orgEnhetEventSourceKafkaListenerContainerFactory",
        clientIdPrefix = "orgEnhetEventSourceListener",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.orgEnhet.eventsourceTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, OrgEnhetEvent> records, Consumer<String, String> consumer) {
        StreamSupport.stream(records.spliterator(), false).forEach(consumerRecord -> {
            var orgEnhetEvent = consumerRecord.value();
            if (orgEnhetEvent instanceof OrgEnhetTypeEndretEventV1 orgEnhetEndretEvent) {
                orgEnhetTypeHandler.orgEnhetsTypeChanged(orgEnhetEndretEvent);
                orgEnhetNomNivaaHandler.mapOrgEnhetTypeToNewNomNivaaAndUpdate(orgEnhetEndretEvent);

                logger.debug(
                    "Listener received message with fid {} and orgEnhetsType {}", orgEnhetEvent.getFid(),
                    orgEnhetEndretEvent.getData().orgEnhetsType());
            }
        });
    }
}
