package no.nav.nom.apps.api.orgenhet.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.common.LocalConfiguration;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.recoverybackup.OrgEnhetEventSource;
import no.nav.nom.apps.api.orgenhet.eventsourcing.recoverybackup.OrgEnhetEventSourceRepository;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class OrgEnhetEventProducer {

    private final KafkaTemplate<String, OrgEnhetEvent> orgEnhetEventProducerTemplate;
    private final OrgEnhetEventSourceRepository orgEnhetEventSourceRepository;
    private final ObjectMapper objectMapper;
    private final boolean shouldSaveToDb;

    public OrgEnhetEventProducer(
            KafkaTemplate<String, OrgEnhetEvent> orgEnhetEventProducerTemplate,
            OrgEnhetEventSourceRepository orgEnhetEventSourceRepository,
            ObjectMapper objectMapper,
            LocalConfiguration localConfiguration
    ) {
        this.orgEnhetEventProducerTemplate = orgEnhetEventProducerTemplate;
        this.orgEnhetEventSourceRepository = orgEnhetEventSourceRepository;
        this.objectMapper = objectMapper;
        this.shouldSaveToDb = localConfiguration.shouldSaveEventsToDb;
    }

    public CompletableFuture<SendResult<String, OrgEnhetEvent>> sendMessage(OrgEnhetEvent event) {
        String eventKey = PrefixedUlidCreator.createEventUlid();
        if(shouldSaveToDb) {
            // Lagring til database gjør applikasjonen utrolig treg ved lokalkjøring, så bypass kan være hensiktsmessig når applikasjonen ikke kjører i miljø.
            saveToDB(event, eventKey);
        }
        return orgEnhetEventProducerTemplate.sendDefault(eventKey, event);
    }

    private void saveToDB(OrgEnhetEvent event, String eventKey) {
        orgEnhetEventSourceRepository.save(OrgEnhetEventSource.builder().eventKey(eventKey).payload(objectMapper.valueToTree(event)).build());
    }
}
