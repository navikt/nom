package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.recoverybackup.OrgtilknytningEventSource;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.recoverybackup.OrgtilknytningEventSourceRepository;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
public class OrgtilknytningEventProducer {

    private final KafkaTemplate<String, OrgtilknytningEvent> orgtilknytningEventProducerTemplate;
    private final OrgtilknytningEventSourceRepository orgtilknytningEventSourceRepository;
    private final ObjectMapper objectMapper;

    public OrgtilknytningEventProducer(
            KafkaTemplate<String, OrgtilknytningEvent> orgtilknytningEventProducerTemplate,
            OrgtilknytningEventSourceRepository orgtilknytningEventSourceRepository,
            ObjectMapper objectMapper
    ) {
        this.orgtilknytningEventProducerTemplate = orgtilknytningEventProducerTemplate;
        this.orgtilknytningEventSourceRepository = orgtilknytningEventSourceRepository;
        this.objectMapper = objectMapper;
    }

    public ListenableFuture<SendResult<String, OrgtilknytningEvent>> sendMessage(OrgtilknytningEvent event) {
        String eventKey = PrefixedUlidCreator.createEventUlid();
        saveToDB(event, eventKey);
        return orgtilknytningEventProducerTemplate.sendDefault(eventKey, event);
    }

    private void saveToDB(OrgtilknytningEvent event, String eventKey) {
        orgtilknytningEventSourceRepository.save(OrgtilknytningEventSource.builder().eventKey(eventKey).payload(objectMapper.valueToTree(event)).build());
    }
}
