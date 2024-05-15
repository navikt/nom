package no.nav.nom.apps.api.orgtilknytning.eventsourcing.state;


import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.lucenesearch.LuceneService;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.utils.common.kafka.KafkaUtils;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class OrgtilknytningStateListener {
    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final String stateTopic;
    private final OrgtilknytningStateRepository orgtilknytningStateService;
    private final Admin adminClient;
    private final KafkaProperties kafkaProperties;
    private final LuceneService luceneService;

    public OrgtilknytningStateListener(
        @Value("${app.kafka.orgtilknytning.state-topic}") String stateTopic,
        @Qualifier("oldOrgtilknytningStateRepository") OrgtilknytningStateRepository orgtilknytningStateService,
        Admin adminClient, LuceneService luceneService,
        KafkaAdmin admin,
        KafkaProperties kafkaProperties
    ) {
        this.stateTopic = stateTopic;
        this.orgtilknytningStateService = orgtilknytningStateService;
        this.adminClient = adminClient;
        this.kafkaProperties = kafkaProperties;
        this.luceneService = luceneService;
    }

    @KafkaListener(
            containerFactory = "orgtilknytningStateKafkaListenerContainerFactory",
            clientIdPrefix = "orgtilknytningStateListener",
            topicPartitions = @TopicPartition(
                    topic = "${app.kafka.orgtilknytning.state-topic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
            )
    )
    public void onMessage(ConsumerRecords<String, OrgtilknytningState> records, Consumer<String, String> consumer) {
        StreamSupport.stream(records.spliterator(), false)
            .forEach( it -> orgtilknytningStateService.update(it.value())); //,it.timestamp();

        KafkaUtils.verifyUpToDate(consumer, stateTopic, initialLoadComplete, () -> log.info("Kafka OrgtilknytningState listener initial load finished - {} Orgtilknytninger", orgtilknytningStateService.count()));
    }

    public boolean initloadComplete() {
        return initialLoadComplete.get();
    }

    public boolean consumerGroupRegistedForTopic(String topic) throws ExecutionException, InterruptedException, TimeoutException {
        final var topicPartitions = adminClient.listConsumerGroupOffsets(kafkaProperties.getConsumer().getGroupId())
                .partitionsToOffsetAndMetadata()
                .get(10, TimeUnit.SECONDS);

        return topicPartitions.entrySet().stream().anyMatch(entry -> entry.getKey().topic().equals(topic));
    }
}
