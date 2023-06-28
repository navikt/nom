package no.nav.nom.apps.api.orgtilknytning.comparenomressurser;


import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.dto.RessursDTO;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.utils.common.kafka.KafkaUtils;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class NomRessurserCompareListener {
    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private static final String NOM_RESSURSER_TOPIC = "org.nom-ressurser";
    private final Admin adminClient;
    private final KafkaProperties kafkaProperties;

    private final HashMap<String, RessursDTO> ressursDTOs = new HashMap<>();

    public NomRessurserCompareListener(
        Admin adminClient,
        KafkaProperties kafkaProperties
    ) {
        this.adminClient = adminClient;
        this.kafkaProperties = kafkaProperties;
    }

    @KafkaListener(
            containerFactory = "nomRessurserListenerContainerFactory",
            clientIdPrefix = "nomRessurserCompareListener",
            topicPartitions = @TopicPartition(
                    topic = NOM_RESSURSER_TOPIC, partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
            )
    )
    public void onMessage(ConsumerRecords<String, RessursDTO> records, Consumer<String, String> consumer) {
        StreamSupport.stream(records.spliterator(), false)
            .forEach(this::collect);

        KafkaUtils.verifyUpToDate(consumer, NOM_RESSURSER_TOPIC, initialLoadComplete, () -> log.info("NOM Ressurser compare listener initial load finished - {} Nom Ressurser DTO's", ressursDTOs.size()));
    }

    private void collect(ConsumerRecord<String, RessursDTO> stringRessursDTOConsumerRecord) {
        String navident = stringRessursDTOConsumerRecord.key();
        RessursDTO ressursDTO = stringRessursDTOConsumerRecord.value();

        ressursDTOs.put(navident, ressursDTO);
    }

    public boolean initloadComplete() {
        if (!initialLoadComplete.get()) {
            try {
                if (!consumerGroupRegistedForTopic(NOM_RESSURSER_TOPIC)) {
                    initialLoadComplete.set(true);
                }
            } catch (Exception exception) {
                log.error("Exception when trying to fetch consumer group from topic", exception);
            }
        }
        return initialLoadComplete.get();
    }

    public boolean consumerGroupRegistedForTopic(String topic) throws ExecutionException, InterruptedException, TimeoutException {
        final var topicPartitions = adminClient.listConsumerGroupOffsets(kafkaProperties.getConsumer().getGroupId())
                .partitionsToOffsetAndMetadata()
                .get(10, TimeUnit.SECONDS);

        return topicPartitions.entrySet().stream().anyMatch(entry -> entry.getKey().topic().equals(topic));
    }
}
