package no.nav.nom.apps.streams.kafkaapi.orgenhet.state;

import no.nav.nom.apps.streams.kafkaapi.orgenhet.state.v3.OrgEnhetState;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetOpprettetEventV1;
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
class EventsrcToState {
    private final static Logger log = LoggerFactory.getLogger(EventsrcToState.class);

    public EventsrcToState(
            @Value("${app.kafka.eventsourceTopic}") String eventsourceTopic,
            @Value("${app.kafka.stateTopic.v3}") String stateTopic,
            JsonMapper jsonMapper,
            JacksonJsonSerde<Object> objectSerde,
            StreamsBuilder streamsBuilder
    ){
        final var stringSerde = Serdes.String();
        final var eventSerde = objectSerde.copyWithType(OrgEnhetEvent.class);

        streamsBuilder.stream(eventsourceTopic, Consumed.with(stringSerde, eventSerde))
                .groupBy((k, v) -> v.getFid(), Grouped.with(stringSerde, eventSerde))
                .aggregate(
                        jsonMapper::createObjectNode,
                        (key, event, state) -> {
                            if(event.getAdditionalData() instanceof OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalAgressoData data){
                                state.set("additionalData", jsonMapper.convertValue(data, ObjectNode.class));
                            }
                            state.setAll(jsonMapper.convertValue(event.getData(), ObjectNode.class));
                            return state;
                        },
                        Materialized.with(stringSerde, objectSerde.copyWithType(ObjectNode.class))
                )
                .mapValues(this::toOrgEnhetState)
                .toStream().to(stateTopic, Produced.with(stringSerde, objectSerde.copyWithType(OrgEnhetState.class)));
    }

    public OrgEnhetState toOrgEnhetState(String fid, JsonNode eventStateAggregate){
        NomNivaa nomNivaa = eventStateAggregate.hasNonNull("nomNivaa") ? NomNivaa.valueOf(eventStateAggregate.get("nomNivaa").asText()) : null;
        OrgEnhetsType orgEnhetsType =  eventStateAggregate.hasNonNull("orgEnhetsType") ? OrgEnhetsType.valueOf(eventStateAggregate.get("orgEnhetsType").asText()) : null;

        try {
            return new OrgEnhetState(
                eventStateAggregate.get("id").asText(),
                eventStateAggregate.get("navn").asText(),
                LocalDate.parse(eventStateAggregate.get("gyldigFom").asText()),
                eventStateAggregate.hasNonNull("gyldigTom") ? LocalDate.parse(eventStateAggregate.get("gyldigTom").asText()) : null,
                nomNivaa,
                orgEnhetsType
            );
        } catch (Exception e){
            log.error("Error mapping to OrgEnhetState", e);
            throw e;
        }
    }
}
