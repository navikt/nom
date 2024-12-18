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
import no.nav.nom.apps.api.orgtilknytning.api.email.OrgtilknytningEndretEmailService;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class OrgtilknytningEvtsrcListener {

    private final OrgtilknytningEndretEmailService orgtilknytningEndretEmailService;
    private final ObjectMapper objectMapper = configureObjectMapper();

    public OrgtilknytningEvtsrcListener(OrgtilknytningEndretEmailService orgtilknytningEndretEmailService) {
        this.orgtilknytningEndretEmailService = orgtilknytningEndretEmailService;
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
        // loggin now for checking
        log.info("OrgtilknytningEvent at nom-api: {}", event);
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
