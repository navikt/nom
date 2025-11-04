package no.nav.nom.apps.api.authorize.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.authorize.eventsourcing.recoverybackup.AuthorizeEventSource;
import no.nav.nom.apps.api.authorize.eventsourcing.recoverybackup.AuthorizeEventSourceRepository;
import no.nav.nom.apps.api.common.LocalConfiguration;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

import static no.nav.nom.libs.utils.common.util.DateTimeUtil.epochMillisNorwegianTimeEndOfDay;

@Service
public class AuthorizeEventProducer {

    private final KafkaTemplate<String, AuthorizationEvent> authorizeEventProducerTemplate;
    private final AuthorizeEventSourceRepository authorizeEventSourceRepository;
    private final ObjectMapper objectMapper;
    private final boolean shouldSaveToDb;

    public AuthorizeEventProducer(
            KafkaTemplate<String, AuthorizationEvent> authorizeEventProducerTemplate,
            AuthorizeEventSourceRepository authorizeEventSourceRepository,
            ObjectMapper objectMapper,
            LocalConfiguration localConfiguration
    ) {
        this.authorizeEventProducerTemplate = authorizeEventProducerTemplate;
        this.authorizeEventSourceRepository = authorizeEventSourceRepository;
        this.objectMapper = objectMapper;
        this.shouldSaveToDb = localConfiguration.shouldSaveEventsToDb;
    }

    public CompletableFuture<SendResult<String, AuthorizationEvent>> sendMessage(AuthorizationEvent event) {
        String eventKey = PrefixedUlidCreator.createEventUlid();
        if(shouldSaveToDb) {
            // Lagring til database gjør applikasjonen utrolig treg ved lokalkjøring, så bypass kan være hensiktsmessig når applikasjonen ikke kjører i miljø.
            saveToDB(event, eventKey);
        }
        return authorizeEventProducerTemplate.sendDefault(eventKey, event);
    }

    public CompletableFuture<SendResult<String, AuthorizationEvent>> replayMessage(String eventKey, AuthorizationEvent event) {
        return authorizeEventProducerTemplate.sendDefault(0, epochMillisNorwegianTimeEndOfDay(event.getMetadata().createdTimestamp()), eventKey, event);
    }

    private void saveToDB(AuthorizationEvent event, String eventKey) {
        authorizeEventSourceRepository.save(AuthorizeEventSource.builder().eventKey(eventKey).payload(objectMapper.valueToTree(event)).build());
    }
}
