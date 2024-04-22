package no.nav.nom.apps.streams.internal.ressurs.eventsrcToState;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
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
            ObjectMapper objectMapper,
            JsonSerde<Object> objectSerde,
            StreamsBuilder streamsBuilder
    ) {
        final var stringSerde = Serdes.String();
        final var eventSerde = objectSerde.copyWithType(RessursEvent.class);

        streamsBuilder.stream(eventsourceTopic, Consumed.with(stringSerde, eventSerde))
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
                                if (event.getAdditionalData() instanceof RessursOpprettetEventV1.RessursOpprettetAdditionalData data) {
                                    if (data.navident().isPresent()) {
                                        state.put("navident", data.navident().get());
                                    }
                                    if (data.startdato().isPresent()) {
                                        state.put("startdato", data.startdato().get().toString());
                                    }
                                }
                            }
                            return state;
                        },
                        Materialized.with(stringSerde, objectSerde.copyWithType(ObjectNode.class))
                )
                .mapValues(this::toRessursState)
                .toStream().to(stateTopic, Produced.with(stringSerde, objectSerde.copyWithType(RessursState.class)));
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

            return new RessursState(
                    fid,
                    eventStateAggregate.get("personident").asText(),
                    eventStateAggregate.get("navident").asText(),
                    Sektor.valueOf(eventStateAggregate.get("sektor").asText()),
                    new Visningsnavn(
                            visningsnavn.get("fornavn").asText(),
                            Optional.ofNullable(parseNullableFromJson(visningsnavn, "mellomnavn")),
                            visningsnavn.get("etternavn").asText()
                    ),
                    Optional.ofNullable(parseNullableFromJson(eventStateAggregate, "primaryTelefonnummer")),
                    Optional.ofNullable(parseNullableFromJson(eventStateAggregate, "sluttdato", LocalDate::parse)),
                    Optional.ofNullable(parseNullableFromJson(eventStateAggregate, "startdato", LocalDate::parse)),
                    Optional.ofNullable(parseNullableFromJson(eventStateAggregate, "primaryFysiskBrukersted")),
                    fysiskBrukerstedList
            );
        } catch (Exception e) {
            log.error("Error mapping to RessursState", e);
            throw e;
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
}
