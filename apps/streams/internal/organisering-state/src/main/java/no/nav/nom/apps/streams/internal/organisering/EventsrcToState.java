package no.nav.nom.apps.streams.internal.organisering;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class EventsrcToState {
    private final static Logger log = LoggerFactory.getLogger(EventsrcToState.class);

    public EventsrcToState(
        @Value("${app.kafka.eventsourceTopic}") String eventsourceTopic,
        @Value("${app.kafka.stateTopic}") String stateTopic,
        ObjectMapper objectMapper,
        JsonSerde<Object> objectSerde,
        StreamsBuilder streamsBuilder
    ) {
        final var stringSerde = Serdes.String();
        final var eventSerde = objectSerde.copyWithType(OrganiseringEvent.class);

        streamsBuilder.stream(eventsourceTopic, Consumed.with(stringSerde, eventSerde))
            .groupBy((k, v) -> v.getFid(), Grouped.with(stringSerde, eventSerde))
            .aggregate(objectMapper::createObjectNode,
                (key, event, state) -> {
                    state.setAll(objectMapper.convertValue(event.getData(), ObjectNode.class));
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
                eventStateAggregate.get("idOver").asText(),
                eventStateAggregate.get("idUnder").asText(),
                eventStateAggregate.get("navnOver").asText(),
                eventStateAggregate.get("navnUnder").asText(),
                Optional.ofNullable(eventStateAggregate.get("gyldigTom")).map(JsonNode::asText).map(LocalDate::parse),
                LocalDate.parse(eventStateAggregate.get("gyldigFom").asText())
            );
        } catch (Exception e){
            log.error("Error mapping to OrganiseringState", e);
            throw e;
        }
    }
}
