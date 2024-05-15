package no.nav.nom.apps.api.orgenhet.eventsourcing.state;


import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.lucenesearch.LuceneService;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
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

import static no.nav.nom.apps.api.orgenhet.eventsourcing.state.ToggledOrgEnhetStateRepository.UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY;

@Slf4j
@Service
public class OrgEnhetStateListener {
    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final String stateTopic;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final Admin adminClient;
    private final KafkaProperties kafkaProperties;
    private final LuceneService luceneService;
    private final Unleash unleash;

    public OrgEnhetStateListener(
        @Value("${app.kafka.orgEnhet.stateTopic}") String stateTopic,
        @Qualifier("oldOrgEnhetStateRepository") OrgEnhetStateRepository orgEnhetStateRepository,
        Admin adminClient, LuceneService luceneService,
        KafkaAdmin admin,
        KafkaProperties kafkaProperties, Unleash unleash
    ) {
        this.stateTopic = stateTopic;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.adminClient = adminClient;
        this.kafkaProperties = kafkaProperties;
        this.luceneService = luceneService;
        this.unleash = unleash;
    }

    @KafkaListener(
            containerFactory = "orgEnhetStateKafkaListenerContainerFactory",
            clientIdPrefix = "orgEnhetStateListener",
            topicPartitions = @TopicPartition(
                    topic = "${app.kafka.orgEnhet.stateTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
            )
    )
    public void onMessage(ConsumerRecords<String, OrgEnhetState> records, Consumer<String, String> consumer) {
        StreamSupport.stream(records.spliterator(), false).forEach(it -> {
            var orgEnhetState = it.value();
            orgEnhetStateRepository.update(orgEnhetState);
            if (!unleash.isEnabled(UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY)) {
                this.luceneService.invalidateOrgenhet(orgEnhetState.id());
            }
        });

        KafkaUtils.verifyUpToDate(consumer, stateTopic, initialLoadComplete, () -> log.info("Kafka OrgEnhetState listener initial load finished - {} OrgEnheter", orgEnhetStateRepository.count()));
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
