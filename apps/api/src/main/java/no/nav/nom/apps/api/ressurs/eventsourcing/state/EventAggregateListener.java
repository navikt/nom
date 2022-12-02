package no.nav.nom.apps.api.ressurs.eventsourcing.state;


import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.lucenesearch.LuceneService;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.utils.common.kafka.KafkaUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class EventAggregateListener {
    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final String stateTopic;
    private final RessursStateService ressursStateService;

    private final Map<String, CompletableFuture<RessursState>> waitingMapByFid = Collections.synchronizedMap(new HashMap<>());

    private final AdminClient adminClient;
    private final KafkaProperties kafkaProperties;
    private final LuceneService luceneService;

    public EventAggregateListener(
            @Value("${app.kafka.ressurs.stateTopic}") String stateTopic,
            RessursStateService ressursStateService,
            LuceneService luceneService,
            KafkaAdmin admin,
            KafkaProperties kafkaProperties
    ) {
        this.stateTopic = stateTopic;
        this.ressursStateService = ressursStateService;
        this.kafkaProperties = kafkaProperties;
        adminClient = AdminClient.create(admin.getConfigurationProperties());
        this.luceneService = luceneService;
    }

    @KafkaListener(
            topicPartitions = @TopicPartition(
                    topic = "${app.kafka.ressurs.stateTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
            )
    )
    public void onMessage(ConsumerRecords<String, RessursState> records, Consumer<String, String> consumer) {
        StreamSupport.stream(records.spliterator(), false).forEach(it -> {
            var ressurs = it.value();
            ressursStateService.putEventAggregate(ressurs);
            this.luceneService.invalidateRessurs(ressurs.navident());
            if (waitingMapByFid.containsKey(it.key())) {
                waitingMapByFid.remove(it.key()).complete(ressurs);

            }
        });

        KafkaUtils.verifyUpToDate(consumer, stateTopic, initialLoadComplete, () -> log.info("Kafka state listener initial load finished"));
    }

    public boolean initloadComplete() {
        if (!initialLoadComplete.get()) {
            try {
                if (!consumerGroupRegistedForTopic(stateTopic)) {
                    initialLoadComplete.set(true);
                }
            } catch (Exception exception) {
                log.error("Exception when trying to fetch consumer group from topic", exception);
            }
        }
        return initialLoadComplete.get();
    }

    public CompletableFuture<RessursState> waitForAggregate(String fid) {
        final CompletableFuture<RessursState> completableFuture = new CompletableFuture<>();
        waitingMapByFid.put(fid, completableFuture);
        return completableFuture;
    }

    public void removeWait(String fid){
        waitingMapByFid.remove(fid);
    }

    public boolean consumerGroupRegistedForTopic(String topic) throws ExecutionException, InterruptedException, TimeoutException {
        final var topicPartitions = adminClient.listConsumerGroupOffsets(kafkaProperties.getConsumer().getGroupId())
                .partitionsToOffsetAndMetadata()
                .get(10, TimeUnit.SECONDS);

        return topicPartitions.entrySet().stream().anyMatch(entry -> entry.getKey().topic().equals(topic));
    }
}
