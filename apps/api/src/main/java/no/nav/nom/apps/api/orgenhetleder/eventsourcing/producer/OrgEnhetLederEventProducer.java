package no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer;

import no.nav.nom.apps.api.common.LocalConfiguration;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.recoverybackup.OrgEnhetLederEventSource;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.recoverybackup.OrgEnhetLederEventSourceRepository;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.CompletableFuture;

import static no.nav.nom.libs.utils.common.util.DateTimeUtil.epochMillisNorwegianTimeEndOfDay;

@Service
public class OrgEnhetLederEventProducer {

    private final KafkaTemplate<String, OrgEnhetLederEvent> orgEnhetLederEventProducerTemplate;
    private final OrgEnhetLederEventSourceRepository orgEnhetLederEventSourceRepository;
    private final JsonMapper jsonMapper;
    private final boolean shouldSaveToDb;

    public OrgEnhetLederEventProducer(
        KafkaTemplate<String, OrgEnhetLederEvent> orgEnhetLederEventProducerTemplate,
        OrgEnhetLederEventSourceRepository orgEnhetLederEventSourceRepository,
        JsonMapper jsonMapper,
        LocalConfiguration localConfiguration
    ) {
        this.orgEnhetLederEventProducerTemplate = orgEnhetLederEventProducerTemplate;
        this.orgEnhetLederEventSourceRepository = orgEnhetLederEventSourceRepository;
        this.jsonMapper = jsonMapper;
        this.shouldSaveToDb = localConfiguration.shouldSaveEventsToDb;
    }

    public CompletableFuture<SendResult<String, OrgEnhetLederEvent>> sendMessage(OrgEnhetLederEvent event) {
        String eventKey = PrefixedUlidCreator.createEventUlid();
        if (shouldSaveToDb) {
            // Lagring til database gjør applikasjonen utrolig treg ved lokalkjøring, så bypass kan være hensiktsmessig når applikasjonen ikke kjører i miljø.
            saveToDB(event, eventKey);
        }
        return orgEnhetLederEventProducerTemplate.sendDefault(eventKey,  event);
    }

    public CompletableFuture<SendResult<String, OrgEnhetLederEvent>> replayMessage(String eventKey, OrgEnhetLederEvent event) {
        return orgEnhetLederEventProducerTemplate.sendDefault(0, epochMillisNorwegianTimeEndOfDay(event.getMetadata().createdTimestamp()), eventKey, event);
    }

    private void saveToDB(OrgEnhetLederEvent event, String eventKey) {
        orgEnhetLederEventSourceRepository.save(OrgEnhetLederEventSource.builder().eventKey(eventKey).payload(jsonMapper.valueToTree(event)).build());
    }
}
