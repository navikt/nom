package no.nav.nom.apps.admin.events.orgtilknytning.eventsource;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class OrgtilknytningEvtsrcListener {

    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private final OrgtilknytningEvtsrcRepository orgtilknytningEvtsrcRepository;
    private final JsonMapper jsonMapper = configureJsonMapper();
    private final String listenerId;

    public OrgtilknytningEvtsrcListener(OrgtilknytningEvtsrcRepository orgtilknytningEvtsrcRepository, String evtsrcOrgtilknytningUniqueListenerId) {
        this.orgtilknytningEvtsrcRepository = orgtilknytningEvtsrcRepository;
        this.listenerId = evtsrcOrgtilknytningUniqueListenerId;
    }

    @KafkaListener(
        containerFactory = "orgtilknytningEvtsrcKafkaListenerContainerFactory",
        id = "#{evtsrcOrgtilknytningUniqueListenerId}",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.orgtilknytning.eventsourceTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
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
        orgtilknytningEvtsrcRepository.addOrgtilknytningEvent(event);
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

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup.get()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            log.info(
                "All Orgtilknytning SourceEvents has now been read on startup. Number of OrgtilknytningEvtsrc messages consumed: {}", orgtilknytningEvtsrcRepository.count()
            );
            hasReadToEndOnStartup.set(true);
        }
    }

    private static JsonMapper configureJsonMapper() {
        return new JsonMapper();
    }
}
