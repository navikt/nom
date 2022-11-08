package no.nav.nom.apps.kafka.aggregate.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Component
class KafkaTransformer {

    public KafkaTransformer(
            @Value("${app.kafka.eventsourceTopic}") String eventsourceTopic,
            @Value("${app.kafka.stateTopic}") String stateTopic,
            ObjectMapper objectMapper,
            JsonSerde<Object> objectSerde,
            StreamsBuilder streamsBuilder
    ){
        final var stringSerde = Serdes.String();
        final var eventSerde = objectSerde.copyWithType(Event.class);

        streamsBuilder.stream(eventsourceTopic, Consumed.with(stringSerde, eventSerde))
                .groupBy((k, v) -> v.getFid().toString(), Grouped.with(stringSerde, eventSerde))
                .aggregate(
                        objectMapper::createObjectNode,
                        (key, event, state) -> {
                            state.setAll(objectMapper.convertValue(event.getData(), ObjectNode.class));
                            return state;
                        },
                        Materialized.with(stringSerde, objectSerde.copyWithType(ObjectNode.class))
                )
                .mapValues((readOnlyKey, value) -> toRessursState(UUID.fromString(readOnlyKey), value))
                .toStream().to(stateTopic, Produced.with(stringSerde, objectSerde.copyWithType(RessursState.class)));
    }

    public RessursState toRessursState(UUID fid, JsonNode eventStateAggregate){
        final var visningsnavn = eventStateAggregate.get("visningsnavn");
        return new RessursState(
                fid,
                eventStateAggregate.get("personident").asText(),
                eventStateAggregate.get("navident").asText(),
                RessursState.Sektor.valueOf(eventStateAggregate.get("sektor").asText()),
                new RessursState.Visningsnavn(
                        visningsnavn.get("fornavn").asText(),
                        Optional.ofNullable(visningsnavn.hasNonNull("mellomnavn") ? visningsnavn.get("mellomnavn").asText() : null),
                        visningsnavn.get("etternavn").asText()
                ),
                Optional.ofNullable(visningsnavn.hasNonNull("sluttdato") ? LocalDate.parse(visningsnavn.get("mellomnavn").asText()) : null)
        );
    }
}
