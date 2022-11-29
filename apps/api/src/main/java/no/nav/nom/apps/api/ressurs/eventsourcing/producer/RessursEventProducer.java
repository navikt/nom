package no.nav.nom.apps.api.ressurs.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.ressurs.eventsourcing.postgresql.RessursEventSource;
import no.nav.nom.apps.api.ressurs.eventsourcing.postgresql.RessursEventSourceRepository;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
public class RessursEventProducer {

    private final KafkaTemplate<String, RessursEvent> ressursEventProducerTemplate;
    private final RessursEventSourceRepository ressursEventSourceRepository;
    private final ObjectMapper objectMapper;

    public RessursEventProducer(KafkaTemplate<String, RessursEvent> ressursEventProducerTemplate, RessursEventSourceRepository ressursEventSourceRepository, ObjectMapper objectMapper){
        this.ressursEventProducerTemplate = ressursEventProducerTemplate;
        this.ressursEventSourceRepository = ressursEventSourceRepository;
        this.objectMapper = objectMapper;
    }

    public ListenableFuture<SendResult<String, RessursEvent>> sendMessage(RessursEvent event) {
        String eventKey = PrefixedUlidCreator.createEventUlid();
        //saveToDB(event, eventKey);
        return ressursEventProducerTemplate.sendDefault(eventKey, event);
    }

    private void saveToDB(RessursEvent event, String eventKey) {
        ressursEventSourceRepository.save(
                RessursEventSource.builder()
                        .eventKey(eventKey)
                        .payload(objectMapper.valueToTree(event))
                .build()
        );
    }
}
