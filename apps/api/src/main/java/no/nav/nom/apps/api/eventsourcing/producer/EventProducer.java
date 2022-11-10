package no.nav.nom.apps.api.eventsourcing.producer;

import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.contract.nom.eventsource.Event;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
public class EventProducer {

    private final KafkaTemplate<String, Event> eventProducerTemplate;

    public EventProducer(KafkaTemplate<String, Event> eventProducerTemplate){
        this.eventProducerTemplate = eventProducerTemplate;
    }

    public ListenableFuture<SendResult<String, Event>> sendMessage(Event event) {
        return eventProducerTemplate.sendDefault(PrefixedUlidCreator.createEventUlid(), event);
    }
}
