package no.nav.nom.apps.streams.internal.ressurs.eventsrcToState;

import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursFysiskeBrukerstederOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursJobbtelefonOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursTekniskSlettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursTilknytningtypeOppdatertV1;
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
import org.springframework.kafka.support.serializer.JacksonJsonSerde;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

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
        JsonMapper jsonMapper,
        JacksonJsonSerde<Object> objectSerde,
        @Qualifier("jsonSerde") JacksonJsonSerde<Object> jsonSerde,
        StreamsBuilder streamsBuilder
    ) {
        final var stringSerde = Serdes.String();
        final var eventSerde = objectSerde.copyWithType(RessursEvent.class);
        final var jsonSerde2 = jsonSerde.copyWithType(ObjectNode.class);

        var groupedObjects = streamsBuilder.stream(eventsourceTopic, Consumed.with(stringSerde, eventSerde))
            .filter((k, v) -> !v.getFid().startsWith("FID_MANUELL")) // Ikke behandle manuelle test ressurser/orgtilknytninger
            .groupBy((k, v) -> v.getFid(), Grouped.with(stringSerde, eventSerde))
            .aggregate(
                jsonMapper::createObjectNode,
                (key, event, state) -> {
                    if (event instanceof RessursTekniskSlettetEventV1) {
                        state.put("TOMBSTONE", true);
                        // return null; // Create TOMBSTONE - should skip mapValues and get forwarded directly to output stream. -
                        // (See https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Stream+Usage+Patterns#KafkaStreamUsagePatterns-Howtomanagethesizeofstatestoresusingtombstones?)
                        // NOPE ! Does not work. If multiple input event at once, it works, but null event alone will be 'reduced' away
                    } else {
                        if (event.getData() == null && event instanceof RessursJobbtelefonOppdatertEventV1 && event.getFid().equals(state.get("fid").asString())) {
                            RessursJobbtelefonOppdatertEventV1 jobbtelefonOppdatertEventV1 = new RessursJobbtelefonOppdatertEventV1(
                                event.getFid(),
                                new RessursJobbtelefonOppdatertEventV1.JobbtelefonOppdatertEventData(
                                    state.get("navident").asString(),
                                    null
                                ),
                                event.getMetadata().inputMetadata()
                            );
                            state.setAll(jsonMapper.convertValue(jobbtelefonOppdatertEventV1.getData(), ObjectNode.class));
                        } else if (event instanceof RessursTilknytningtypeOppdatertV1) {
                            var evtSrcFeltVerdier = jsonMapper.convertValue(event.getData(), ObjectNode.class);
                            evtSrcFeltVerdier.remove("sektor");// Denne eventtypen skulle aldri hatt sektor i dataene sine. Dette var en typo feil.
                            state.setAll(evtSrcFeltVerdier);
                        } else {
                            state.setAll(jsonMapper.convertValue(event.getData(), ObjectNode.class));
                        }
                        state.put("fid", event.getFid()); // store this for later retrieval
                        if (event.getAdditionalData() instanceof RessursOpprettetEventV1.RessursOpprettetAdditionalData data) {
                            if (data.navident() != null) {
                                state.put("navident", data.navident());
                            }
                            if (data.startdato() != null) {
                                state.put("startdato", data.startdato().toString());
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
            .groupBy((k, v) -> v.get("navident").asString(), Grouped.with(stringSerde, jsonSerde2))
            .aggregate(
                jsonMapper::createObjectNode,
                (k, v, x) -> v,
                Materialized.with(stringSerde, jsonSerde2)
            );

        var identType = streamsBuilder.stream(navidentTopic, Consumed.with(stringSerde, stringSerde))
            .mapValues(v -> navidentTopicValueToJson(v, jsonMapper))
            .groupBy(
                (k, v) -> v.get("payload").get("navident").asString(),
                Grouped.with(stringSerde, jsonSerde2)
            )
            .aggregate(
                () -> "",
                (key, event, state) -> event.get("payload").get("identType").asString(),
                Materialized.with(stringSerde, stringSerde)
            );

        var withIdentTypeByNavident = byNavIdentGroupedObjects.leftJoin(
            identType, (a, b) -> {
                a.put("identType", b);
                return a;
            }
        );

        var withIdentTypeByFid = withIdentTypeByNavident.toStream().groupBy((k, v) -> v.get("fid").asString(), Grouped.with(stringSerde, jsonSerde2))
            .aggregate(
                jsonMapper::createObjectNode,
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

    private static ObjectNode navidentTopicValueToJson(String value, JsonMapper jsonMapper) {
        try {
            var parsedObjNode = jsonMapper.readValue(value, ObjectNode.class);
            var payload = parsedObjNode.get("payload");
            var hasExpectedFields = payload.has("identType") && payload.has("navident");
            if (!hasExpectedFields) {
                throw new RuntimeException("JSON fra navident topic hadde ikke-forventede felter");
            }
            return parsedObjNode;
        } catch (JacksonException e) {
            throw new RuntimeException("Feil ved JSON parsing fra navident topic");
        }
    }

    private static <T> T parseNullableFromJson(JsonNode jsonNode, String key, Function<String, T> transform) {
        return keyIsNonNullAndEksluderOptionalEmptyText(jsonNode, key) ? transform.apply(jsonNode.get(key).asString()) : null;
    }

    // Ser ut til at vi i stream prosessing state har fått startdato="Optional.empty" som string. Dette må kunne prosesseres, men bør kunne tas vekk på senere tidspunkt.
    // Fordi dette vil 'forsvinne' når mellomstegene er behandlet, og bør ikke oppstå igjen
    private static boolean keyIsNonNullAndEksluderOptionalEmptyText(JsonNode jsonNode, String key) {
        return jsonNode.hasNonNull(key) && !jsonNode.get(key).asString().equals("Optional.empty");
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
            if (eventStateAggregate.hasNonNull("sektor")) {
                try {
                    Sektor.valueOf(eventStateAggregate.get("sektor").asString());
                } catch (IllegalArgumentException e) {
                    log.warn("Forkaster unparsable sektor '{}' fra eventState aggregate", eventStateAggregate.get("sektor").asString());
                    ((ObjectNode)eventStateAggregate).remove("sektor");
                }
            }

            final var visningsnavn = eventStateAggregate.get("visningsnavn");
            final var jobbtelefon = eventStateAggregate.hasNonNull("jobbtelefon") ? eventStateAggregate.get("jobbtelefon") : null;

            var fysiskBrukerstedList = new ArrayList<String>();
            eventStateAggregate.withArray("fysiskeBrukersteder").elements().iterator().forEachRemaining(e -> fysiskBrukerstedList.add(e.asString()));

            var identTypeFromJson = parseNullableFromJson(eventStateAggregate, "identType");

            return new RessursState(
                fid,
                eventStateAggregate.get("personident").asString(),
                eventStateAggregate.get("navident").asString(),
                eventStateAggregate.hasNonNull("sektor") ? Sektor.valueOf(eventStateAggregate.get("sektor").asString()) : Sektor.NAV_STATLIG,
                new Visningsnavn(
                    visningsnavn.get("fornavn").asString(),
                    Optional.ofNullable(parseNullableFromJson(visningsnavn, "mellomnavn")),
                    visningsnavn.get("etternavn").asString()
                ),
                jobbtelefon != null  ? new Jobbtelefon(
                    jobbtelefon.hasNonNull("nummer") ? jobbtelefon.get("nummer").asString() : null,
                    jobbtelefon.hasNonNull("type") ? TelefonType.valueOf(jobbtelefon.get("type").asString()) : null
                ) : null,
                parseNullableFromJson(eventStateAggregate, "primaryTelefonnummer"),
                parseNullableFromJson(eventStateAggregate, "sluttdato", LocalDate::parse),
                parseNullableFromJson(eventStateAggregate, "startdato", LocalDate::parse),
                parseNullableFromJson(eventStateAggregate, "primaryFysiskBrukersted"),
                fysiskBrukerstedList,

                identTypeFromJson != null ? IdentType.valueOf(identTypeFromJson) : null,
                eventStateAggregate.hasNonNull("sistEndretAv") ? eventStateAggregate.get("sistEndretAv").asString() : null,
                eventStateAggregate.hasNonNull("tilknytningtype") ? Tilknytningtype.valueOf(eventStateAggregate.get("tilknytningtype").asString()) : Tilknytningtype.ANNET
            );
        } catch (Exception e) {
            log.error("Error mapping to RessursState", e);
            throw e;
        }
    }
}
