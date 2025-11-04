package no.nav.nom.apps.api.ressurs.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.common.LocalConfiguration;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.ressurs.eventsourcing.recoverybackup.RessursEventSource;
import no.nav.nom.apps.api.ressurs.eventsourcing.recoverybackup.RessursEventSourceRepository;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

import static no.nav.nom.libs.utils.common.util.DateTimeUtil.epochMillisNorwegianTimeEndOfDay;

@Service
public class RessursEventProducer {

    private final KafkaTemplate<String, RessursEvent> ressursEventProducerTemplate;
    private final RessursEventSourceRepository ressursEventSourceRepository;
    private final ObjectMapper objectMapper;

    private final boolean shouldSaveToDb;

    public RessursEventProducer(
            KafkaTemplate<String, RessursEvent> ressursEventProducerTemplate,
            RessursEventSourceRepository ressursEventSourceRepository,
            ObjectMapper objectMapper,
            LocalConfiguration localConfiguration
    ) {
        this.ressursEventProducerTemplate = ressursEventProducerTemplate;
        this.ressursEventSourceRepository = ressursEventSourceRepository;
        this.objectMapper = objectMapper;
        this.shouldSaveToDb = localConfiguration.shouldSaveEventsToDb;
    }

    public CompletableFuture<SendResult<String, RessursEvent>> sendMessage(RessursEvent event) {
        String eventKey = PrefixedUlidCreator.createEventUlid();
        if(shouldSaveToDb) {
            // Lagring til database gjør applikasjonen utrolig treg ved lokalkjøring, så bypass kan være hensiktsmessig når applikasjonen ikke kjører i miljø.
            saveToDB(event, eventKey);
        }
        return ressursEventProducerTemplate.sendDefault(eventKey, event);
    }

    public CompletableFuture<SendResult<String, RessursEvent>> replayMessage(String eventKey, RessursEvent event) {
        return ressursEventProducerTemplate.sendDefault(0, epochMillisNorwegianTimeEndOfDay(event.getMetadata().createdTimestamp()), eventKey, event);
    }

    private void saveToDB(RessursEvent event, String eventKey) {
        ressursEventSourceRepository.save(RessursEventSource.builder().eventKey(eventKey).payload(objectMapper.valueToTree(event)).build());
    }
}
