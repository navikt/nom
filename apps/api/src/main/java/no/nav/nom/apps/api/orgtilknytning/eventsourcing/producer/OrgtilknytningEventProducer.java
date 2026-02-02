package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer;

import no.nav.nom.apps.api.common.LocalConfiguration;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.recoverybackup.OrgtilknytningEventSource;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.recoverybackup.OrgtilknytningEventSourceRepository;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.CompletableFuture;

import static no.nav.nom.libs.utils.common.util.DateTimeUtil.epochMillisNorwegianTimeEndOfDay;

@Service
public class OrgtilknytningEventProducer {

    private final KafkaTemplate<String, OrgtilknytningEvent> orgtilknytningEventProducerTemplate;
    private final OrgtilknytningEventSourceRepository orgtilknytningEventSourceRepository;
    private final JsonMapper jsonMapper;
    private final boolean shouldSaveToDb;

    public OrgtilknytningEventProducer(
            KafkaTemplate<String, OrgtilknytningEvent> orgtilknytningEventProducerTemplate,
            OrgtilknytningEventSourceRepository orgtilknytningEventSourceRepository,
            JsonMapper jsonMapper,
            LocalConfiguration localConfiguration
    ) {
        this.orgtilknytningEventProducerTemplate = orgtilknytningEventProducerTemplate;
        this.orgtilknytningEventSourceRepository = orgtilknytningEventSourceRepository;
        this.jsonMapper = jsonMapper;
        this.shouldSaveToDb = localConfiguration.shouldSaveEventsToDb;
    }

    public CompletableFuture<SendResult<String, OrgtilknytningEvent>> sendMessage(OrgtilknytningEvent event) {
        String eventKey = PrefixedUlidCreator.createEventUlid();
        if(shouldSaveToDb) {
            // Lagring til database gjør applikasjonen utrolig treg ved lokalkjøring, så bypass kan være hensiktsmessig når applikasjonen ikke kjører i miljø.
            saveToDB(event, eventKey);
        }
        return orgtilknytningEventProducerTemplate.sendDefault(eventKey, event);
    }

    public CompletableFuture<SendResult<String, OrgtilknytningEvent>> replayMessage(String eventKey, OrgtilknytningEvent event) {
        return orgtilknytningEventProducerTemplate.sendDefault(0, epochMillisNorwegianTimeEndOfDay(event.getMetadata().createdTimestamp()), eventKey, event);
    }

    private void saveToDB(OrgtilknytningEvent event, String eventKey) {
        orgtilknytningEventSourceRepository.save(OrgtilknytningEventSource.builder().eventKey(eventKey).payload(jsonMapper.valueToTree(event)).build());
    }
}
