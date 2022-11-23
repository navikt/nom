package no.nav.nom.apps.api.ressurs.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.ressurs.eventsourcing.postgresql.RessursEventSource;
import no.nav.nom.apps.api.ressurs.eventsourcing.postgresql.RessursEventSourceRepository;
import no.nav.nom.contract.nom.eventsource.Event;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
public class EventProducer {

    private final KafkaTemplate<String, Event> ressursEventProducerTemplate;
    private final RessursEventSourceRepository ressursEventSourceRepository;
    private final ObjectMapper objectMapper;

    public EventProducer(KafkaTemplate<String, Event> ressursEventProducerTemplate, RessursEventSourceRepository ressursEventSourceRepository, ObjectMapper objectMapper){
        this.ressursEventProducerTemplate = ressursEventProducerTemplate;
        this.ressursEventSourceRepository = ressursEventSourceRepository;
        this.objectMapper = objectMapper;
    }

    public ListenableFuture<SendResult<String, Event>> sendMessage(Event event) {
        String eventKey = PrefixedUlidCreator.createEventUlid();
        saveToDB(event, eventKey);
        return ressursEventProducerTemplate.sendDefault(eventKey, event);
    }

    private void saveToDB(Event event, String eventKey) {
        ressursEventSourceRepository.save(
                RessursEventSource.builder()
                        .eventKey(eventKey)
                        .payload(objectMapper.valueToTree(event))
                .build()
        );
    }
}
