package no.nav.nom.libs.state.ressurs;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.event.ListenerContainerIdleEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

@Slf4j
public class NomStateRessursListener {
    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private boolean started = false;
    private final String stateTopic;
    private final RessursStateRepository ressursStateRepository;
    private final String listenerId;
    private final Map<String, CompletableFuture<RessursState>> waitingMapByFid = Collections.synchronizedMap(new HashMap<>());
    private final KafkaProperties kafkaProperties;
    private final List<Visitor> visitors = new CopyOnWriteArrayList<>();

    public NomStateRessursListener(
        String stateTopic,
        RessursStateRepository ressursStateRepository,
        String listenerId,
        KafkaProperties kafkaProperties
    ) {
        this.stateTopic = stateTopic;
        this.ressursStateRepository = ressursStateRepository;
        this.listenerId = listenerId;
        this.kafkaProperties = kafkaProperties;
    }

    public void register(Visitor visitor) {
        log.info("Registered visitor for NomStateRessursListener with name: {}", visitor.visitorName());
        visitors.add(visitor);
    }

    public interface Visitor {
        String visitorName();

        void visit(RessursState ressursState);
    }

    @KafkaListener(
        id = "#{ressursStateListenerId.toString()}",
        containerFactory = "nomStateRessursKafkaListenerContainerFactory",
        topicPartitions = @TopicPartition(
            topic = "${app.nom-state.kafka.ressurs.state-topic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, RessursState> records) {
        if (!started) {
            log.info("Kafka RessursState listener starting. {} visitors registered: {}", visitors.size(), visitorsToString());
            started = true;
        }
        StreamSupport.stream(records.spliterator(), false).forEach(it -> {
            processMessage(it.key(), it.value());
        });
    }

    private void notifyVisitors(RessursState ressursState) {
        visitors.forEach(v -> v.visit(ressursState));
    }

    private String visitorsToString() {
        return visitors.stream().map(Visitor::visitorName).reduce((a, b) -> a + ", " + b).orElse("none");
    }

    private void processMessage(String key, RessursState ressursState) {
        if (ressursState == null) {
            ressursStateRepository.remove(key);
        } else {
            ressursStateRepository.update(ressursState);
        }
        if (waitingMapByFid.containsKey(key)) {
            waitingMapByFid.remove(key).complete(ressursState);
            log.info("Done waiting for FID in NomStateRessursListener: {}", key);
        }
        notifyVisitors(ressursState);
    }

    public CompletableFuture<RessursState> waitForStateAfterEventCreation(String fid) {
        log.info("Waiting for FID in NomStateRessursListener: {}", fid);
        final CompletableFuture<RessursState> completableFuture = new CompletableFuture<>();
        waitingMapByFid.put(fid, completableFuture);
        return completableFuture;
    }

    public void removeWait(String fid) {
        waitingMapByFid.remove(fid);
    }

    public boolean hasReadToEndOnStartup() {
        return hasReadToEndOnStartup.get();
    }

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup.get()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            log.info("All SourceEvents has now been read on startup. Number of Ressurser: {} in [{}]", ressursStateRepository.count(), ressursStateRepository);
            hasReadToEndOnStartup.set(true);
        }
    }
}
