package no.nav.nom.apps.api.organisering.eventsourcing.producer;

import no.nav.nom.apps.api.common.LocalConfiguration;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.organisering.eventsourcing.recoverybackup.OrganiseringEventSource;
import no.nav.nom.apps.api.organisering.eventsourcing.recoverybackup.OrganiseringEventSourceRepository;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.CompletableFuture;

import static no.nav.nom.libs.utils.common.util.DateTimeUtil.epochMillisNorwegianTimeEndOfDay;

@Service
public class OrganiseringEventProducer {
    private final KafkaTemplate<String, OrganiseringEvent> organiseringEventProducerTemplate;
    private final OrganiseringEventSourceRepository organiseringEventSourceRepository;
    private final JsonMapper jsonMapper;
    private final boolean shouldSaveToDb;

    public OrganiseringEventProducer(
        KafkaTemplate<String, OrganiseringEvent> organiseringEventProducerTemplate,
        OrganiseringEventSourceRepository organiseringEventSourceRepository,
        JsonMapper jsonMapper,
        LocalConfiguration localConfiguration
        ) {
        this.organiseringEventProducerTemplate = organiseringEventProducerTemplate;
        this.organiseringEventSourceRepository = organiseringEventSourceRepository;
        this.jsonMapper = jsonMapper;
        this.shouldSaveToDb = localConfiguration.shouldSaveEventsToDb;
    }

    public CompletableFuture<SendResult<String, OrganiseringEvent>> sendMessage(OrganiseringEvent event) {
        String eventKey = PrefixedUlidCreator.createEventUlid();
        if(shouldSaveToDb) {
            // Lagring til database gjør applikasjonen utrolig treg ved lokalkjøring,
            // så bypass kan være hensiktsmessig når applikasjonen ikke kjører i miljø.
            saveToDB(event, eventKey);
        }
        return organiseringEventProducerTemplate.sendDefault(eventKey, event);
    }

    public CompletableFuture<SendResult<String, OrganiseringEvent>> replayMessage(String eventKey, OrganiseringEvent event) {
        return organiseringEventProducerTemplate.sendDefault(0, epochMillisNorwegianTimeEndOfDay(event.getMetadata().createdTimestamp()), eventKey, event);
    }

    private void saveToDB(OrganiseringEvent event, String eventKey) {
        organiseringEventSourceRepository.save(OrganiseringEventSource.builder().eventKey(eventKey).payload(jsonMapper.valueToTree(event)).build());
    }
}
