package no.nav.nom.libs.state.authorize;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
import no.nav.nom.contract.nom.eventsource.DelegertAuthorizationAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.DelegertAuthorizationFornyetEventV1;
import no.nav.nom.contract.nom.eventsource.DelegertAuthorizationOpprettetEventV1;
import no.nav.nom.contract.nom.state.LederTilgangState;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import no.nav.nom.libs.utils.common.kafka.KafkaUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;

import java.time.LocalDateTime;

@Slf4j
public class NomStateDelegertLederListener extends AbstractNomStateListener<AuthorizationEvent> {
    private final DelegertLederStateRepository delegertLederStateRepository;

    public NomStateDelegertLederListener(String stateTopic, DelegertLederStateRepository delegertLederStateRepository, String listenerId) {
        super(stateTopic, listenerId);
        this.delegertLederStateRepository = delegertLederStateRepository;
    }

    @KafkaListener(
        id = "#{delegertLederStateListenerId}",
        containerFactory = "nomStateDelegertLederKafkaListenerContainerFactory",
        topicPartitions = @TopicPartition(
            topic = "${nom.libs.state.delegert-leder.state-topic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, AuthorizationEvent> records) {
        super.onMessage(records);
    }

    @Override
    protected void registerMessageWithState(String key, AuthorizationEvent authorizationEvent) {
        if (authorizationEvent instanceof DelegertAuthorizationOpprettetEventV1) {
            process((DelegertAuthorizationOpprettetEventV1) authorizationEvent);
        } else if (authorizationEvent instanceof DelegertAuthorizationFornyetEventV1) {
            process((DelegertAuthorizationFornyetEventV1) authorizationEvent);
        } else if (authorizationEvent instanceof DelegertAuthorizationAvsluttetEventV1) {
            process((DelegertAuthorizationAvsluttetEventV1) authorizationEvent);
        } else {
            log.warn("Unknown event type: {}", authorizationEvent.getClass().getName());
        }
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

    @Override
    protected void doneReadToEndOnStartup() {
        log.info("Number of DelegertLeder read on startup: {}", delegertLederStateRepository.count());
    }
}
