package no.nav.nom.apps.api.authorize.eventsourcing.state;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.DelegertAuthorizationAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.DelegertAuthorizationFornyetEventV1;
import no.nav.nom.contract.nom.eventsource.DelegertAuthorizationOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.state.LederTilgangState;
import no.nav.nom.libs.utils.common.kafka.KafkaUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@KafkaListener(
    containerFactory = "delegertLederKafkaListenerContainerFactory",
    clientIdPrefix = "delegertLederStateListener",
    topicPartitions = @TopicPartition(
        topic = "${app.kafka.authorize.eventsource-topic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
    )
)
public class DelegertLederEventListener {
    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final String evtTopic;
    private final OldDelegertLederStateRepositoryImpl delegertLederStateRepository;

    private final List<Visitor> visitors = new CopyOnWriteArrayList<>();

    public DelegertLederEventListener(@Value("${app.kafka.authorize.eventsource-topic}") String evtTopic, OldDelegertLederStateRepositoryImpl delegertLederStateRepository) {
        this.evtTopic = evtTopic;
        this.delegertLederStateRepository = delegertLederStateRepository;
    }

    @KafkaHandler
    public void handler(DelegertAuthorizationOpprettetEventV1 opprettEvt, Consumer<String, String> consumer) {
        log.info("Received DelegertAuthorizationOpprettetEventV1: {}", opprettEvt.getFid());
        process(opprettEvt);

        KafkaUtils.verifyUpToDate(consumer, evtTopic, initialLoadComplete, () -> log.info("Kafka DelegertLederEventListener listener initial load finished. {} delegerte leder roller lastet", delegertLederStateRepository.count()));
        notifyVisitors(opprettEvt);
    }

    @KafkaHandler
    public void handler(DelegertAuthorizationFornyetEventV1 fornyetEvt, Consumer<String, String> consumer) {
        log.info("Received DelegertAuthorizationFornyetEventV1: {}", fornyetEvt.getFid());
        process(fornyetEvt);

        KafkaUtils.verifyUpToDate(consumer, evtTopic, initialLoadComplete, () -> log.info("Kafka DelegertLederEventListener listener initial load finished. {} delegerte leder roller lastet", delegertLederStateRepository.count()));
        notifyVisitors(fornyetEvt);
    }

    @KafkaHandler
    public void handler(DelegertAuthorizationAvsluttetEventV1 avsluttetEvt, Consumer<String, String> consumer) {
        log.info("Received DelegertAuthorizationAvsluttetEventV1: {}", avsluttetEvt.getFid());
        process(avsluttetEvt);

        KafkaUtils.verifyUpToDate(consumer, evtTopic, initialLoadComplete, () -> log.info("Kafka DelegertLederEventListener listener initial load finished. {} delegerte leder roller lastet", delegertLederStateRepository.count()));
        notifyVisitors(avsluttetEvt);
    }

    private void process(DelegertAuthorizationOpprettetEventV1 opprettet) {
        var eventData = (DelegertAuthorizationOpprettetEventV1.DelegertAuthorizationOpprettetEventData)opprettet.getData();
        var newState = new LederTilgangState(opprettet.getFid(), eventData.til(), eventData.av(), eventData.forOrgenhet(), eventData.gyldigFom(), eventData.gyldigTom());

        delegertLederStateRepository.update(newState);
    }

    private void process(DelegertAuthorizationFornyetEventV1 fornyet) {
        var eventData = (DelegertAuthorizationFornyetEventV1.DelegertAuthorizationFornyetEventData)fornyet.getData();
        var newState = new LederTilgangState(fornyet.getFid(), eventData.til(), eventData.av(), eventData.forOrgenhet(), eventData.gyldigFom(), eventData.nyGyldigTom());

        delegertLederStateRepository.update(newState);
    }

    private void process(DelegertAuthorizationAvsluttetEventV1 avsluttet){
        var eventData = (DelegertAuthorizationAvsluttetEventV1.DelegertAuthorizationAvsluttetEventData)avsluttet.getData();
        var newState = new LederTilgangState(avsluttet.getFid(), eventData.til(), eventData.av(), eventData.forOrgenhet(), eventData.gyldigFom(), eventData.nyGyldigTom());

        delegertLederStateRepository.update(newState);
    }

    public boolean initloadComplete() {
        return initialLoadComplete.get();
    }

    public void register(Visitor visitor) {
        visitors.add(visitor);
    }

    private void notifyVisitors(Event evt) {
        visitors.forEach(v -> v.visit(evt));
    }

    public interface Visitor {
        void visit(Event event);
    }
}
