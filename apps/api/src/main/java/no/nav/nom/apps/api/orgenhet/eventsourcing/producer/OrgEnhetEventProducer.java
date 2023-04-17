package no.nav.nom.apps.api.orgenhet.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.recoverybackup.OrgEnhetEventSource;
import no.nav.nom.apps.api.orgenhet.eventsourcing.recoverybackup.OrgEnhetEventSourceRepository;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
public class OrgEnhetEventProducer {

    private final KafkaTemplate<String, OrgEnhetEvent> orgEnhetEventProducerTemplate;
    private final OrgEnhetEventSourceRepository orgEnhetEventSourceRepository;
    private final ObjectMapper objectMapper;

    public OrgEnhetEventProducer(
            KafkaTemplate<String, OrgEnhetEvent> orgEnhetEventProducerTemplate,
            OrgEnhetEventSourceRepository orgEnhetEventSourceRepository,
            ObjectMapper objectMapper
    ) {
        this.orgEnhetEventProducerTemplate = orgEnhetEventProducerTemplate;
        this.orgEnhetEventSourceRepository = orgEnhetEventSourceRepository;
        this.objectMapper = objectMapper;
    }

    public ListenableFuture<SendResult<String, OrgEnhetEvent>> sendMessage(OrgEnhetEvent event) {
        String eventKey = PrefixedUlidCreator.createEventUlid();
        saveToDB(event, eventKey);
        return orgEnhetEventProducerTemplate.sendDefault(eventKey, event);
    }

    private void saveToDB(OrgEnhetEvent event, String eventKey) {
        orgEnhetEventSourceRepository.save(OrgEnhetEventSource.builder().eventKey(eventKey).payload(objectMapper.valueToTree(event)).build());
    }
}
