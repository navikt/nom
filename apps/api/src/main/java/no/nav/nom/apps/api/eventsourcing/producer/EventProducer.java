package no.nav.nom.apps.api.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.ressurs.eventsourcing.postgresql.EventSource;
import no.nav.nom.apps.api.ressurs.eventsourcing.postgresql.EventSourceRepository;
import no.nav.nom.contract.nom.eventsource.Event;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
public class EventProducer {

    private final KafkaTemplate<String, Event> eventProducerTemplate;
    private final EventSourceRepository eventSourceRepository;
    private final ObjectMapper objectMapper;

    public EventProducer(KafkaTemplate<String, Event> eventProducerTemplate, EventSourceRepository eventSourceRepository, ObjectMapper objectMapper){
        this.eventProducerTemplate = eventProducerTemplate;
        this.eventSourceRepository = eventSourceRepository;
        this.objectMapper = objectMapper;
    }

    public ListenableFuture<SendResult<String, Event>> sendMessage(Event event) {
        String eventKey = PrefixedUlidCreator.createEventUlid();
        saveToDB(event, eventKey);
        return eventProducerTemplate.sendDefault(eventKey, event);
    }

    private void saveToDB(Event event, String eventKey) {
        eventSourceRepository.save(
                EventSource.builder()
                        .eventKey(eventKey)
                        .payload(objectMapper.valueToTree(event))
                .build()
        );
    }
}
