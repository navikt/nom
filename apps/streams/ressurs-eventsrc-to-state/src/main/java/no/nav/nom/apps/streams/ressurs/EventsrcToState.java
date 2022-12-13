package no.nav.nom.apps.streams.ressurs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
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
import java.util.Optional;

@Component
class EventsrcToState {

    private final static Logger log = LoggerFactory.getLogger(EventsrcToState.class);

    public EventsrcToState(
            @Value("${app.kafka.eventsourceTopic}") String eventsourceTopic,
            @Value("${app.kafka.stateTopic}") String stateTopic,
            ObjectMapper objectMapper,
            JsonSerde<Object> objectSerde,
            StreamsBuilder streamsBuilder
    ){
        final var stringSerde = Serdes.String();
        final var eventSerde = objectSerde.copyWithType(RessursEvent.class);

        streamsBuilder.stream(eventsourceTopic, Consumed.with(stringSerde, eventSerde))
                .groupBy((k, v) -> v.getFid(), Grouped.with(stringSerde, eventSerde))
                .aggregate(
                        objectMapper::createObjectNode,
                        (key, event, state) -> {
                            if(event.getAdditionalData() instanceof RessursOpprettetEventV1.RessursOpprettetAdditionalData data){
                                if(data.navident().isPresent()){
                                    state.put("navident", data.navident().get());
                                }
                            }
                            state.setAll(objectMapper.convertValue(event.getData(), ObjectNode.class));
                            return state;
                        },
                        Materialized.with(stringSerde, objectSerde.copyWithType(ObjectNode.class))
                )
                .mapValues(this::toRessursState)
                .toStream().to(stateTopic, Produced.with(stringSerde, objectSerde.copyWithType(RessursState.class)));
    }

    public RessursState toRessursState(String fid, JsonNode eventStateAggregate){
        try {
            final var visningsnavn = eventStateAggregate.get("visningsnavn");

            return new RessursState(
                    fid,
                    eventStateAggregate.get("personident").asText(),
                    eventStateAggregate.get("navident").asText(),
                    Sektor.valueOf(eventStateAggregate.get("sektor").asText()),
                    new Visningsnavn(
                            visningsnavn.get("fornavn").asText(),
                            Optional.ofNullable(visningsnavn.hasNonNull("mellomnavn") ? visningsnavn.get("mellomnavn").asText() : null),
                            visningsnavn.get("etternavn").asText()
                    ),
                    Optional.ofNullable(eventStateAggregate.hasNonNull("sluttdato") ? LocalDate.parse(eventStateAggregate.get("sluttdato").asText()) : null)
            );
        } catch (Exception e){
            log.error("Error mapping to RessursState", e);
            throw e;
        }
    }
}
