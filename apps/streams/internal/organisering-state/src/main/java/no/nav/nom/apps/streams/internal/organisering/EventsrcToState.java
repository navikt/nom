package no.nav.nom.apps.streams.internal.organisering;

import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;

@Component
public class EventsrcToState {
    private final static Logger log = LoggerFactory.getLogger(EventsrcToState.class);

    public EventsrcToState(
        @Value("${app.kafka.eventsourceTopic}") String eventsourceTopic,
        @Value("${app.kafka.stateTopic}") String stateTopic,
        JsonMapper jsonMapper,
        JacksonJsonSerde<Object> objectSerde,
        StreamsBuilder streamsBuilder
    ) {
        final var stringSerde = Serdes.String();
        final var eventSerde = objectSerde.copyWithType(OrganiseringEvent.class);

        streamsBuilder.stream(eventsourceTopic, Consumed.with(stringSerde, eventSerde))
            .groupBy((k, v) -> v.getFid(), Grouped.with(stringSerde, eventSerde))
                .aggregate(
                        jsonMapper::createObjectNode,
                (key, event, state) -> {
                    state.setAll(jsonMapper.convertValue(event.getData(), ObjectNode.class));
                    return state;
                },
                Materialized.with(stringSerde, objectSerde.copyWithType(ObjectNode.class))
            )
            .mapValues(this::toOrganiseringState)
            .toStream().to(stateTopic, Produced.with(stringSerde, objectSerde.copyWithType(OrganiseringState.class)));
    }


    public OrganiseringState toOrganiseringState(String fid, JsonNode eventStateAggregate){
        try {
            return new OrganiseringState(
                fid,
                    eventStateAggregate.get("id").asString(),
                    eventStateAggregate.get("navn").asString(),
                eventStateAggregate.get("idOver").asString(),
                eventStateAggregate.get("navnOver").asString(),
                LocalDate.parse(eventStateAggregate.get("gyldigFom").asText()),
                eventStateAggregate.hasNonNull("gyldigTom") ? LocalDate.parse(eventStateAggregate.get("gyldigTom").asText()) : null
            );
        } catch (Exception e){
            log.error("Error mapping to OrganiseringState", e);
            throw e;
        }
    }
}
