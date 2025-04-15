package no.nav.nom.apps.streams.internal.ressurs.eventsrcToState;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursFysiskeBrukerstederOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursTekniskSlettetEventV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

@Component
class EventsrcToState {

    private final static Logger log = LoggerFactory.getLogger(EventsrcToState.class);

    public EventsrcToState(
        @Value("${app.kafka.eventsourceTopic}") String eventsourceTopic,
        @Value("${app.kafka.stateTopic}") String stateTopic,
        @Value("${app.kafka.navidentTopic}") String navidentTopic,
        ObjectMapper objectMapper,
        JsonSerde<Object> objectSerde,
        @Qualifier("jsonSerde") JsonSerde<Object> jsonSerde,
        StreamsBuilder streamsBuilder
    ) {
        final var stringSerde = Serdes.String();
        final var eventSerde = objectSerde.copyWithType(RessursEvent.class);
        final var jsonSerde2 = jsonSerde.copyWithType(ObjectNode.class);

        var groupedObjects = streamsBuilder.stream(eventsourceTopic, Consumed.with(stringSerde, eventSerde))
            .filter((k, v) -> !v.getFid().startsWith("FID_MANUELL")) // Ikke behandle manuelle test ressurser/orgtilknytninger
            .groupBy((k, v) -> v.getFid(), Grouped.with(stringSerde, eventSerde))
            .aggregate(
                objectMapper::createObjectNode,
                (key, event, state) -> {
                    if (event instanceof RessursTekniskSlettetEventV1) {
                        state.put("TOMBSTONE", true);
                        // return null; // Create TOMBSTONE - should skip mapValues and get forwarded directly to output stream. -
                        // (See https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Stream+Usage+Patterns#KafkaStreamUsagePatterns-Howtomanagethesizeofstatestoresusingtombstones?)
                        // NOPE ! Does not work. If multiple input event at once, it works, but null event alone will be 'reduced' away
                    } else {
                        state.setAll(objectMapper.convertValue(event.getData(), ObjectNode.class));
                        state.put("fid", event.getFid()); // store this for later retrieval
                        if (event.getAdditionalData() instanceof RessursOpprettetEventV1.RessursOpprettetAdditionalData data) {
                            if (data.navident().isPresent()) {
                                state.put("navident", data.navident().get());
                            }
                            if (data.startdato().isPresent()) {
                                state.put("startdato", data.startdato().get().toString());
                            }
                        }
                        leggTilFysiskBrukerstedArrayIfMissing(event, state);
                        if (event.getMetadata() != null && event.getMetadata().inputMetadata() != null) {
                            var origination = event.getMetadata().inputMetadata().origination();
                            if (origination != null && origination.initiatedBy() != null) {
                                state.put("sistEndretAv", origination.initiatedBy());
                            }
                        }
                    }
                    return state;
                },
                Materialized.with(stringSerde, objectSerde.copyWithType(ObjectNode.class))
            );

        var byNavIdentGroupedObjects = groupedObjects.toStream()
            .filter((k, v) -> v != null)
            .groupBy((k, v) -> v.get("navident").asText(), Grouped.with(stringSerde, jsonSerde2))
            .aggregate(
                objectMapper::createObjectNode,
                (k, v, x) -> v,
                Materialized.with(stringSerde, jsonSerde2)
            );

        var identType = streamsBuilder.stream(navidentTopic, Consumed.with(stringSerde, stringSerde))
            .mapValues(v -> navidentTopicValueToJson(v, objectMapper))
            .groupBy(
                (k, v) -> v.get("payload").get("navident").asText(),
                Grouped.with(stringSerde, jsonSerde2)
            )
            .aggregate(
                () -> "",
                (key, event, state) -> event.get("payload").get("identType").asText(),
                Materialized.with(stringSerde, stringSerde)
            );

        var withIdentTypeByNavident = byNavIdentGroupedObjects.leftJoin(
            identType, (a, b) -> {
                a.put("identType", b);
                return a;
            }
        );

        var withIdentTypeByFid = withIdentTypeByNavident.toStream().groupBy((k, v) -> v.get("fid").asText(), Grouped.with(stringSerde, jsonSerde2))
            .aggregate(
                objectMapper::createObjectNode,
                (key, value, aggregate) -> value,
                Materialized.with(stringSerde, jsonSerde2)
            );

        withIdentTypeByFid.mapValues(this::toRessursState)
            .toStream().to(stateTopic, Produced.with(stringSerde, objectSerde.copyWithType(RessursState.class)));
    }

    private static void leggTilFysiskBrukerstedArrayIfMissing(RessursEvent event, ObjectNode state) {
        if (!state.has("fysiskeBrukersteder") || (state.get("fysiskeBrukersteder") == null) || !state.get("fysiskeBrukersteder").isArray()) {
            if (event instanceof RessursFysiskeBrukerstederOppdatertEventV1) {
                log.warn("Legger til 'fysiskeBrukersteder' felt for ressurs-state fid " + event.getFid());
            }
            state.putArray("fysiskeBrukersteder");
        }
    }

    private static ObjectNode navidentTopicValueToJson(String value, ObjectMapper objectMapper) {
        try {
            var parsedObjNode = objectMapper.readValue(value, ObjectNode.class);
            var payload = parsedObjNode.get("payload");
            var hasExpectedFields = payload.has("identType") && payload.has("navident");
            if (!hasExpectedFields) {
                throw new RuntimeException("JSON fra navident topic hadde ikke-forventede felter");
            }
            return parsedObjNode;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Feil ved JSON parsing fra navident topic");
        }
    }

    private static <T> T parseNullableFromJson(JsonNode jsonNode, String key, Function<String, T> transform) {
        return keyIsNonNullAndEksluderOptionalEmptyText(jsonNode, key) ? transform.apply(jsonNode.get(key).asText()) : null;
    }

    // Ser ut til at vi i stream prosessing state har fått startdato="Optional.empty" som string. Dette må kunne prosesseres, men bør kunne tas vekk på senere tidspunkt.
    // Fordi dette vil 'forsvinne' når mellomstegene er behandlet, og bør ikke oppstå igjen
    private static boolean keyIsNonNullAndEksluderOptionalEmptyText(JsonNode jsonNode, String key) {
        return jsonNode.hasNonNull(key) && !jsonNode.get(key).asText().equals("Optional.empty");
    }

    private static String parseNullableFromJson(JsonNode jsonNode, String key) {
        return parseNullableFromJson(jsonNode, key, Function.identity());
    }

    public RessursState toRessursState(String fid, JsonNode eventStateAggregate) {
        try {
            if (eventStateAggregate.hasNonNull("TOMBSTONE")) {
                log.info("SlettRessursEvent for fid {} received - create TOMBSTONE in StateTopic", fid);
                return null;
            }

            final var visningsnavn = eventStateAggregate.get("visningsnavn");

            var fysiskBrukerstedList = new ArrayList<String>();
            eventStateAggregate.withArray("fysiskeBrukersteder").elements().forEachRemaining(e -> fysiskBrukerstedList.add(e.asText()));

            var identTypeFromJson = parseNullableFromJson(eventStateAggregate, "identType");

            return new RessursState(
                fid,
                eventStateAggregate.get("personident").asText(),
                eventStateAggregate.get("navident").asText(),
                eventStateAggregate.hasNonNull("sektor") ? Sektor.valueOf(eventStateAggregate.get("sektor").asText()) : Sektor.NAV_STATLIG,
                new Visningsnavn(
                    visningsnavn.get("fornavn").asText(),
                    Optional.ofNullable(parseNullableFromJson(visningsnavn, "mellomnavn")),
                    visningsnavn.get("etternavn").asText()
                ),
                Optional.ofNullable(parseNullableFromJson(eventStateAggregate, "primaryTelefonnummer")),
                Optional.ofNullable(parseNullableFromJson(eventStateAggregate, "sluttdato", LocalDate::parse)),
                Optional.ofNullable(parseNullableFromJson(eventStateAggregate, "startdato", LocalDate::parse)),
                Optional.ofNullable(parseNullableFromJson(eventStateAggregate, "primaryFysiskBrukersted")),
                fysiskBrukerstedList,

                identTypeFromJson != null ? IdentType.valueOf(identTypeFromJson) : null,
                eventStateAggregate.hasNonNull("sistEndretAv") ? eventStateAggregate.get("sistEndretAv").asText() : null
            );
        } catch (Exception e) {
            log.error("Error mapping to RessursState", e);
            throw e;
        }
    }
}
