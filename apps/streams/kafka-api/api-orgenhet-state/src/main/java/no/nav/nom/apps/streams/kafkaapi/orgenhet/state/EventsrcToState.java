package no.nav.nom.apps.streams.kafkaapi.orgenhet.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.apps.streams.kafkaapi.orgenhet.state.v2.OrgEnhetState;
import no.nav.nom.contract.nom.OrgEnhetsKategori;
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
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
class EventsrcToState {
    private final static Logger log = LoggerFactory.getLogger(EventsrcToState.class);

    public EventsrcToState(
            @Value("${app.kafka.eventsourceTopic}") String eventsourceTopic,
            @Value("${app.kafka.stateTopic.v2}") String stateTopic,
            ObjectMapper objectMapper,
            JsonSerde<Object> objectSerde,
            StreamsBuilder streamsBuilder
    ){
        final var stringSerde = Serdes.String();
        final var eventSerde = objectSerde.copyWithType(OrgEnhetEvent.class);

        streamsBuilder.stream(eventsourceTopic, Consumed.with(stringSerde, eventSerde))
                .groupBy((k, v) -> v.getFid(), Grouped.with(stringSerde, eventSerde))
                .aggregate(
                        objectMapper::createObjectNode,
                        (key, event, state) -> {
                            if(event.getAdditionalData() instanceof OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalAgressoData data){
                                state.set("additionalData", objectMapper.convertValue(data, ObjectNode.class));
                            }
                            state.setAll(objectMapper.convertValue(event.getData(), ObjectNode.class));
                            return state;
                        },
                        Materialized.with(stringSerde, objectSerde.copyWithType(ObjectNode.class))
                )
                .mapValues(this::toOrgEnhetState)
                .toStream().to(stateTopic, Produced.with(stringSerde, objectSerde.copyWithType(OrgEnhetState.class)));
    }

    public OrgEnhetState toOrgEnhetState(String fid, JsonNode eventStateAggregate){
        Optional<OrgEnhetsKategori> orgEnhetsKategori = Optional.ofNullable(eventStateAggregate.hasNonNull("orgEnhetsKategori") ? OrgEnhetsKategori.valueOf(eventStateAggregate.get("orgEnhetsKategori").asText()) : null);

        try {
            return new OrgEnhetState(
                eventStateAggregate.get("id").asText(),
                eventStateAggregate.get("navn").asText(),
                LocalDate.parse(eventStateAggregate.get("gyldigFom").asText()),
                Optional.ofNullable(eventStateAggregate.hasNonNull("gyldigTom") ? LocalDate.parse(eventStateAggregate.get("gyldigTom").asText()) : null),
                orgEnhetsKategori
            );
        } catch (Exception e){
            log.error("Error mapping to OrgEnhetState", e);
            throw e;
        }
    }
}
