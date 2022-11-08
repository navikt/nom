package no.nav.nom.apps.api.eventsourcing.state;


import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.utils.common.kafka.KafkaUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class EventAggregateListener {
    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final String stateTopic;
    private final RessursStateService ressursStateService;

    private final Map<UUID, Mono<RessursState>> waitingMapByFid = new ConcurrentHashMap<>();

    public EventAggregateListener(@Value("${app.kafka.stateTopic}") String stateTopic, RessursStateService ressursStateService){
        this.stateTopic = stateTopic;
        this.ressursStateService = ressursStateService;
    }

    @KafkaListener(
            topicPartitions = @TopicPartition(topic = "${app.kafka.stateTopic}",
            partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")),
            groupId = "nom-api"
    )
    public void onMessage(ConsumerRecords<String, RessursState> records, Consumer<String, String> consumer){
        StreamSupport.stream(records.spliterator(), false)
                .forEach(r -> {
                    ressursStateService.putEventAggregate(r.value());
                    if(waitingMapByFid.containsKey(UUID.fromString(r.key()))) {
                        waitingMapByFid.remove(UUID.fromString(r.key())).thenReturn(r.value());
                    }
                });

        KafkaUtils.verifyUpToDate(consumer, stateTopic, initialLoadComplete, () -> log.info("Kafka state listener initial load finished"));
    }

    public boolean initloadComplete(){
        return initialLoadComplete.get();
    }

    public Mono<RessursState> waitForAggregate(UUID fid){
        return waitingMapByFid.put(fid, Mono.empty());
    }
}
