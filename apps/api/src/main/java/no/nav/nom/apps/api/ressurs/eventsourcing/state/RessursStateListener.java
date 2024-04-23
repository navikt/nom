package no.nav.nom.apps.api.ressurs.eventsourcing.state;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.lucenesearch.LuceneService;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
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
public class RessursStateListener {
    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final String stateTopic;
    private final RessursStateRepository ressursStateRepository;

    private final Map<String, CompletableFuture<RessursState>> waitingMapByFid = Collections.synchronizedMap(new HashMap<>());

    private final Admin adminClient;
    private final KafkaProperties kafkaProperties;
    private final LuceneService luceneService;
    private final Unleash unleash;

    public RessursStateListener(
            @Value("${app.kafka.ressurs.stateTopic}") String stateTopic,
            @Qualifier("oldRessursStateRepository") RessursStateRepository ressursStateRepository,
            Admin adminClient, LuceneService luceneService,
            KafkaAdmin admin,
            KafkaProperties kafkaProperties, Unleash unleash
    ) {
        this.stateTopic = stateTopic;
        this.ressursStateRepository = ressursStateRepository;
        this.adminClient = adminClient;
        this.kafkaProperties = kafkaProperties;
        this.luceneService = luceneService;
        this.unleash = unleash;
    }

    @KafkaListener(
        containerFactory = "ressursStateKafkaListenerContainerFactory",
        clientIdPrefix = "ressursStateListener",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.ressurs.stateTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, RessursState> records, Consumer<String, String> consumer) {
        StreamSupport.stream(records.spliterator(), false).forEach(it -> {
            processMessage(it.key(), it.value());
        });

        KafkaUtils.verifyUpToDate(
            consumer,
            stateTopic,
            initialLoadComplete,
            () -> log.info("Kafka RessursState listener initial load finished. {} ressurser in [{}]", ressursStateRepository.count(), ressursStateRepository)
        );
    }

    private void processMessage(String key, RessursState ressursState) {
        if (ressursState == null) {
            ressursStateRepository.remove(key);
        } else {
            ressursStateRepository.update(ressursState);
            if (!unleash.isEnabled(LuceneService.UNLEASH_USE_NEW_RESSURS_STATE_LISTENER)) {
                this.luceneService.invalidateRessurs(ressursState.navident());
            }
        }
        if (waitingMapByFid.containsKey(key)) {
            waitingMapByFid.remove(key).complete(ressursState);
        }
    }

    public boolean initloadComplete() {
        return initialLoadComplete.get();
    }

    public CompletableFuture<RessursState> waitForAggregate(String fid) {
        final CompletableFuture<RessursState> completableFuture = new CompletableFuture<>();
        waitingMapByFid.put(fid, completableFuture);
        return completableFuture;
    }

    public void removeWait(String fid) {
        waitingMapByFid.remove(fid);
    }

    public boolean consumerGroupRegistedForTopic(String topic) throws ExecutionException, InterruptedException, TimeoutException {
        final var topicPartitions = adminClient.listConsumerGroupOffsets(kafkaProperties.getConsumer().getGroupId())
            .partitionsToOffsetAndMetadata()
            .get(10, TimeUnit.SECONDS);

        return topicPartitions.entrySet().stream().anyMatch(entry -> entry.getKey().topic().equals(topic));
    }
}
