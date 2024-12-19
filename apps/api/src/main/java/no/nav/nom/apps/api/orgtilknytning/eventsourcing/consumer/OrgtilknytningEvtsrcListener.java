package no.nav.nom.apps.api.orgtilknytning.eventsourcing.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgtilknytning.api.email.OTEndretData;
import no.nav.nom.apps.api.orgtilknytning.api.email.OrgtilknytningEndretEmailService;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFjernetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class OrgtilknytningEvtsrcListener {

    private final Environment env;
    private final OrgtilknytningEndretEmailService orgtilknytningEndretEmailService;
    private final NomMastered nomMastered;
    private final ObjectMapper objectMapper = configureObjectMapper();

    public OrgtilknytningEvtsrcListener(
        OrgtilknytningEndretEmailService orgtilknytningEndretEmailService,
        Environment env,
        NomMastered nomMastered) {
        this.orgtilknytningEndretEmailService = orgtilknytningEndretEmailService;
        this.env = env;
        this.nomMastered = nomMastered;
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
            event = objectMapper.readValue(jsonString, OrgtilknytningEvent.class);
        } catch (Exception e) {
            log.info("Failed to parse OrgtilknytningEvent from message: {}", jsonString, e);
            log.info("Trying to correct MIDLERTIDLIG til MIDLERTIDIG in json before mapping Json to event object");
            try {
                event = objectMapper.treeToValue(replaceIllegalEnumMidlertidlig(jsonString), OrgtilknytningEvent.class);
            } catch (JsonProcessingException ex) {
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
            log.info("OT-endringer operation is not performed nom frontend. OTendretData won´t be added to the email queue");
            return;
        }

        String initiatedBy = event.getMetadata().inputMetadata().origination().initiatedBy();
        String navident =  orgtilknytningEndretEmailService.getNavidentFromEvent(event);

        if(navident == null || navident.isBlank()) {
            log.warn("Failed to get navident from OrgtilknytningEvent event: {}", event);
            return;
        }

        if(!nomMastered.isMastered(navident)) {
            log.info("Navident {} is not mastered in nom. OTendretData won´t be added to the email queue", navident);
            return;
        }

        // loggin now for checking
        log.info("Adding to the OT changes email cache for navident: {}, event: {}", navident, event);
        orgtilknytningEndretEmailService.addToOrgtilknytningEndringerQueue(
            navident,
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
            jsonNode = objectMapper.readTree(jsonString);
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse json from message", ex);
            throw new RuntimeException(ex);
        }

        var tilknytningstypeNode = jsonNode.findValue("orgtilknytningType");
        if (tilknytningstypeNode != null) {
            var tilknytningstype = tilknytningstypeNode.asText();
            if ("MIDLERTIDLIG".equals(tilknytningstype)) {
                ((ObjectNode)jsonNode.findParent("orgtilknytningType")).put("orgtilknytningType", "MIDLERTIDIG");
                log.info("New json: {}", jsonNode);
            }
        } else {
            log.error("Failed to find 'orgtilknytningType' in json");
        }
        return jsonNode;
    }

    private static ObjectMapper configureObjectMapper() {
        final var objectMapper = new ObjectMapper();

        return objectMapper.findAndRegisterModules()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
}
