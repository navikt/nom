package no.nav.nom.apps.streams.internal.orgtilknytning.eventsrctostate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFjernetEventV1;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
class KafkaStreamProcessor {

    private final static Logger log = LoggerFactory.getLogger(KafkaStreamProcessor.class);

    public KafkaStreamProcessor(
        @Value("${app.kafka.eventsourceTopic}") String eventsourceTopic,
        @Value("${app.kafka.stateTopic}") String stateTopic,
        ObjectMapper objectMapper,
        JsonSerde<Object> objectSerde,
        StreamsBuilder streamsBuilder
    ) {
        final var stringSerde = Serdes.String();
        final var eventSerde = objectSerde.copyWithType(OrgtilknytningEvent.class);
        streamsBuilder.stream(eventsourceTopic, Consumed.with(stringSerde, eventSerde))
            .groupBy((k, v) -> v.getFid(), Grouped.with(stringSerde, eventSerde))
            .aggregate(
                objectMapper::createObjectNode,
                (key, event, state) -> {
                    if (event instanceof OrgtilknytningFjernetEventV1) {
                        state.put("TOMBSTONE", true);
                        // return null; // Create TOMBSTONE - should skip mapValues and get forwarded directly to output stream. -
                        // (See https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Stream+Usage+Patterns#KafkaStreamUsagePatterns-Howtomanagethesizeofstatestoresusingtombstones?)
                        // NOPE ! Does not work. If multiple input event at once, it works, but null event alone will be 'reduced' away
                    } else {
                        state.setAll(objectMapper.convertValue(event.getData(), ObjectNode.class));
                    }
                    return state;
                },
                Materialized.with(stringSerde, objectSerde.copyWithType(ObjectNode.class))
            )
            .mapValues(this::toOrgtilknytningState)
            .toStream()
            .peek((k, v) -> log.info("Log aggregate [{} - {}]", k, v.navident()))
            .to(stateTopic, Produced.with(stringSerde, objectSerde.copyWithType(OrgtilknytningState.class)));
    }

    public OrgtilknytningState toOrgtilknytningState(String fid, JsonNode eventStateAggregate) {
        try {
            if (eventStateAggregate.hasNonNull("TOMBSTONE")) {
                return null;
            }
            return new OrgtilknytningState(
                fid,
                eventStateAggregate.get("navident").asText(),
                eventStateAggregate.get("orgEnhetId").asText(),
                Boolean.parseBoolean(eventStateAggregate.hasNonNull("dagligOppfolging") ? eventStateAggregate.get("dagligOppfolging").asText() : null),
                LocalDate.parse(eventStateAggregate.get("gyldigFom").asText()),
                Optional.ofNullable(eventStateAggregate.hasNonNull("gyldigTom") ? LocalDate.parse(eventStateAggregate.get("gyldigTom").asText()) : null),
                Optional.ofNullable(eventStateAggregate.findValue("orgtilknytningType")).map(s -> Tilknytningtype.valueOf(s.asText()))
                    .or(() -> Optional.ofNullable(eventStateAggregate.findValue("tilknytningtype")).map(s -> Tilknytningtype.valueOf(s.asText())))
            );
        } catch (Exception e) {
            log.error("Error mapping to OrgtilknytningState", e);
            throw e;
        }
    }
}
