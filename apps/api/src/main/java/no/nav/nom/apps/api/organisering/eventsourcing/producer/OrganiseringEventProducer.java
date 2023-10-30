package no.nav.nom.apps.api.organisering.eventsourcing.producer;

import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class OrganiseringEventProducer {

    private final KafkaTemplate<String, OrganiseringEvent> organiseringEventProducerTemplate;

    public OrganiseringEventProducer(KafkaTemplate<String, OrganiseringEvent> organiseringEventProducerTemplate) {
        this.organiseringEventProducerTemplate = organiseringEventProducerTemplate;
    }

    public CompletableFuture<SendResult<String, OrganiseringEvent>> sendMessage(OrganiseringEvent event) {
        String eventKey = PrefixedUlidCreator.createEventUlid();

        return organiseringEventProducerTemplate.sendDefault(eventKey, event);
    }
}
