package no.nav.nom.apps.api.orgtilknytning.api.email;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFjernetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class OrgtilknytningEvtsrcListener {
    
    private final Environment env;
    private final OrgtilknytningEndretEmailService orgtilknytningEndretEmailService;
    private final JsonMapper jsonMapper = configureJsonMapper();

    public OrgtilknytningEvtsrcListener(OrgtilknytningEndretEmailService orgtilknytningEndretEmailService, Environment env) {
        this.orgtilknytningEndretEmailService = orgtilknytningEndretEmailService;
        this.env = env;
    }

    @KafkaListener(
        containerFactory = "orgtilknytningEvtsrcKafkaListenerContainerFactory",
        id = "nomApi_orgtilknytningEventSourceHandler",
        clientIdPrefix = "orgtilknytningEventSourceListener",
        topics = "${app.kafka.orgtilknytning.eventsource-topic}"
    )
    public void onMessage(ConsumerRecords<String, String> records) {
        StreamSupport.stream(records.spliterator(), false)
            .map(ConsumerRecord::value)
            .forEach(this::handleMessage);
    }

    private void handleMessage(String jsonString) {
        OrgtilknytningEvent event = null;
        try {
            event = jsonMapper.readValue(jsonString, OrgtilknytningEvent.class);
        } catch (Exception e) {
            log.info("Failed to parse OrgtilknytningEvent from message: {}", jsonString, e);
            log.info("Trying to correct MIDLERTIDLIG til MIDLERTIDIG in json before mapping Json to event object");
            try {
                event = jsonMapper.treeToValue(replaceIllegalEnumMidlertidlig(jsonString), OrgtilknytningEvent.class);
            } catch (JacksonException ex) {
                log.error("Failed to map json to event", ex);
                throw new RuntimeException(ex);
            }
        }

        // process event
        addToOTEndretDataEmailQueue(event);
    }

    private void addToOTEndretDataEmailQueue(OrgtilknytningEvent event) {
        String initiatedFrom = event.getMetadata().inputMetadata().origination().initiatedFrom();
        if(!initiatedFrom.equals(getClusterName()+":org:nom-ui2")) {
            log.debug("OT-endringer operation is not performed nom frontend: {}. OTendretData won´t be added to the email queue", initiatedFrom);
            return;
        }

        String initiatedBy = event.getMetadata().inputMetadata().origination().initiatedBy();
        String navident =  orgtilknytningEndretEmailService.getNavidentFromOrgtilknytningStateRepository(event.getFid());

        // loggin now for checking
        log.info("Adding to the OT changes email cache for fid: {}, event: {}", event.getFid(), event);
        orgtilknytningEndretEmailService.addToOrgtilknytningEndringerQueue(
            event.getFid(),
            OTEndretData.builder()
                .fid(event.getFid())
                .navident(navident)
                .changedByNavIdent(initiatedBy)
                .eventType(getOtEndretDataType(event))
                .timestamp(DateTimeFormatter.ofPattern("dd.MM.yyyy").format(LocalDate.now()))
                .build()
        );
    }

    private static OTEndretData.OTEndretDataType getOtEndretDataType(OrgtilknytningEvent event) {
        OTEndretData.OTEndretDataType eventType;
        switch (event) {
            case OrgtilknytningOpprettetEventV1 ignored->
                eventType = OTEndretData.OTEndretDataType.OT_NY;
            case OrgtilknytningAvsluttetEventV1 ignored ->
                eventType = OTEndretData.OTEndretDataType.OT_AVSLUTTET;
            case OrgtilknytningFjernetEventV1 ignored ->
                eventType = OTEndretData.OTEndretDataType.OT_FJERNET;
            default ->
                eventType =OTEndretData.OTEndretDataType.OT_ENDRET;
        }
        return eventType;
    }

    private String getClusterName() {
        var isProd = "prod-gcp".equals(env.getProperty("NAIS_CLUSTER_NAME"));
        if(isProd) return "prod-gcp";
        return "dev-gcp";
    }

    protected JsonNode replaceIllegalEnumMidlertidlig(String jsonString) {
        JsonNode jsonNode;
        try {
            jsonNode = jsonMapper.readTree(jsonString);
        } catch (JacksonException ex) {
            log.error("Failed to parse json from message", ex);
            throw new RuntimeException(ex);
        }

        var tilknytningstypeNode = jsonNode.findValue("orgtilknytningType");
        if (tilknytningstypeNode != null) {
            var tilknytningstype = tilknytningstypeNode.asString();
            if ("MIDLERTIDLIG".equals(tilknytningstype)) {
                ((ObjectNode)jsonNode.findParent("orgtilknytningType")).put("orgtilknytningType", "MIDLERTIDIG");
                log.info("New json: {}", jsonNode);
            }
        } else {
            log.error("Failed to find 'orgtilknytningType' in json");
        }
        return jsonNode;
    }

    private static JsonMapper configureJsonMapper() {
        final var jsonMapper = new JsonMapper();

        return jsonMapper.rebuild().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
    }
}
