package no.nav.nom.apps.api.ressurs.eventsourcing.postgresql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
@Slf4j
public class RessursEventSourceRecovery {

    private final KafkaTemplate<String, RessursEvent> ressursEventProducerTemplate;
    private final RessursEventSourceRepository ressursEventSourceRepository;
    private final ObjectMapper objectMapper;

    public RessursEventSourceRecovery(
            @Qualifier("new_RessursEventSourceTopic") KafkaTemplate<String, RessursEvent> ressursEventProducerTemplate_newTopic,
            RessursEventSourceRepository ressursEventSourceRepository,
            ObjectMapper objectMapper
    ) {
        this.ressursEventProducerTemplate = ressursEventProducerTemplate_newTopic;
        this.ressursEventSourceRepository = ressursEventSourceRepository;
        this.objectMapper = objectMapper;
    }

    private ListenableFuture<SendResult<String, RessursEvent>> sendMessage(String eventKey, RessursEvent event) {
        return ressursEventProducerTemplate.sendDefault(eventKey, event);
    }

    public void recoverFromDB() {
        ressursEventSourceRepository.findAll().forEach( es -> {
            String eventKey = es.getEventKey();
            try {
                RessursEvent event = objectMapper.treeToValue(es.getPayload(), RessursEvent.class);
                sendMessage(eventKey, event);
            } catch (JsonProcessingException e) {
                log.error("Failed to convert DB backed-up Ressurs EventSource with key - %s".formatted(eventKey), e);
            }
        });
    }
}
