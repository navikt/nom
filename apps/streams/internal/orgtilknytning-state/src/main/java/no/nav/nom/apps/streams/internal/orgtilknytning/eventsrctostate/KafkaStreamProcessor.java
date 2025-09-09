package no.nav.nom.apps.streams.internal.orgtilknytning.eventsrctostate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFjernetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1;
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
            .filter((k, v) -> !v.getFid().startsWith("FID_MANUELL")) // Ikke behandle manuelle test ressurser/orgtilknytninger
            .groupBy((k, v) -> v.getFid(), Grouped.with(stringSerde, eventSerde))
            .aggregate(
                objectMapper::createObjectNode,
                (key, event, state) -> {
                    var convertedEvent = objectMapper.convertValue(event.getData(), ObjectNode.class);
                    if (event instanceof OrgtilknytningFjernetEventV1) {
                        state.put("TOMBSTONE", true);
                        // return null; // Create TOMBSTONE - should skip mapValues and get forwarded directly to output stream. -
                        // (See https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Stream+Usage+Patterns#KafkaStreamUsagePatterns-Howtomanagethesizeofstatestoresusingtombstones?)
                        // NOPE ! Does not work. If multiple input event at once, it works, but null event alone will be 'reduced' away
                    } else {
                        state.setAll(convertedEvent);
                        if (event.getMetadata() != null && event.getMetadata().inputMetadata() != null) {
                            var origination = event.getMetadata().inputMetadata().origination();
                            if (origination != null && origination.initiatedBy() != null) {
                                state.put("sistEndretAv", origination.initiatedBy());
                            }
                        }
                    }
                    fjernMasseflyttFraFidHvisIkkeAngittIEvent(state, convertedEvent);
                    return state;
                },
                Materialized.with(stringSerde, objectSerde.copyWithType(ObjectNode.class))
            )
            .mapValues(this::toOrgtilknytningState)
            .toStream()
            .to(stateTopic, Produced.with(stringSerde, objectSerde.copyWithType(OrgtilknytningState.class)));
    }

    private void fjernMasseflyttFraFidHvisIkkeAngittIEvent(ObjectNode state, ObjectNode event) {
        if (state.has("masseflyttetFraFid")) {
            if (!eventHarMasseflyttFraFidSatt(event)) {
                state.remove("masseflyttetFraFid");
            }
        }
    }

    private boolean eventHarMasseflyttFraFidSatt(ObjectNode event) {
        return event.hasNonNull("masseflyttetFraFid");
    }

    public OrgtilknytningState toOrgtilknytningState(String fid, JsonNode eventStateAggregate) {
        try {
            if (eventStateAggregate.hasNonNull("TOMBSTONE")) {
                return null;
            }

            log.info("Mapping to OrgtilknytningState: {}", eventStateAggregate.toPrettyString());

            var tilknytningType = getTilknytningtype(eventStateAggregate);

            return new OrgtilknytningState(
                fid,
                eventStateAggregate.get("navident").asText(),
                eventStateAggregate.get("orgEnhetId").asText(),
                eventStateAggregate.hasNonNull("sektor") ? Sektor.valueOf(eventStateAggregate.get("sektor").asText()) : null,
                Boolean.parseBoolean(eventStateAggregate.hasNonNull("dagligOppfolging") ? eventStateAggregate.get("dagligOppfolging").asText() : null),
                LocalDate.parse(eventStateAggregate.get("gyldigFom").asText()),
                eventStateAggregate.hasNonNull("gyldigTom") ? LocalDate.parse(eventStateAggregate.get("gyldigTom").asText()) : null,
                tilknytningType,
                eventStateAggregate.hasNonNull("masseflyttetFraFid") ? eventStateAggregate.get("masseflyttetFraFid").asText() : null,
                eventStateAggregate.hasNonNull("sistEndretAv") ? eventStateAggregate.get("sistEndretAv").asText() : null
            );
        } catch (Exception e) {
            log.error("Error mapping to OrgtilknytningState", e);
            throw e;
        }
    }

    private static Tilknytningtype getTilknytningtype(JsonNode eventStateAggregate) {
        var tilknytningstypeNode = eventStateAggregate.findValue("orgtilknytningType");
        tilknytningstypeNode = tilknytningstypeNode == null ? eventStateAggregate.findValue("tilknytningtype") : tilknytningstypeNode;

        var tilknytningstypeText = tilknytningstypeNode == null || tilknytningstypeNode.isNull() ? null : tilknytningstypeNode.asText();
        tilknytningstypeText = "MIDLERTIDLIG".equals(tilknytningstypeText) ? "MIDLERTIDIG" : tilknytningstypeText;

        var tilknytningtype = tilknytningstypeText == null ? null : Tilknytningtype.valueOf(tilknytningstypeText);
        return tilknytningtype;
    }
}
