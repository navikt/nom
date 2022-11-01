package no.nav.nom.apps.api.eventsourcing.state;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.utils.common.kafka.KafkaUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class EventAggregateListener {

    public static final String CACHE = "cache-source-kafka";
    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final String stateTopic;
    private final KafkaStateRessursService kafkaStateRessursService;

    private final ApplicationEventPublisher applicationEventPublisher;

    public EventAggregateListener(@Value("${app.kafka.stateTopic}") String stateTopic, KafkaStateRessursService kafkaStateRessursService, ObjectMapper objectMapper, ApplicationEventPublisher applicationEventPublisher){
        this.stateTopic = stateTopic;
        this.kafkaStateRessursService = kafkaStateRessursService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @KafkaListener(topicPartitions =
            @TopicPartition(topic = "${app.kafka.stateTopic}",
            partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")),
    groupId = "nom-api")
    void onMessage(ConsumerRecords<String, RessursState> records, Consumer<String, String> consumer){
        StreamSupport.stream(records.spliterator(), false)
                .forEach(r -> {
                    kafkaStateRessursService.putEventAggregate(r.value());
                });

        kafkaStateRessursService.getAll();


        KafkaUtils.verifyUpToDate(consumer, stateTopic, initialLoadComplete, () -> {
            log.info("Kafka state listener initial load finished");
        });
    }

    public boolean initloadComplete(){
        return initialLoadComplete.get();
    }
}
