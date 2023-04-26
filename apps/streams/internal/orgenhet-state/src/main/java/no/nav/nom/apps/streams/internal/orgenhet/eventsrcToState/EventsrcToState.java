package no.nav.nom.apps.streams.internal.orgenhet.eventsrcToState;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.contract.nom.OrgEnhetsKategori;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetOpprettetEventV1;
import no.nav.nom.contract.nom.state.OrgEnhetState;
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
        Optional<String> overordnetOrgEnhetFid = Optional.ofNullable(eventStateAggregate.hasNonNull("overordnetOrgEnhetFid") ? eventStateAggregate.get("overordnetOrgEnhetFid").asText() : null);
        Optional<String> lederFid = Optional.ofNullable(eventStateAggregate.hasNonNull("lederRessursFid") ? eventStateAggregate.get("lederRessursFid").asText() : null);

        Optional<OrgEnhetsKategori> orgEnhetsKategori = Optional.ofNullable(eventStateAggregate.hasNonNull("nomNivaa") ? OrgEnhetsKategori.valueOf(eventStateAggregate.get("nomNivaa").asText()) : null);
        Optional<OrgEnhetsType> orgEnhetsType =  Optional.ofNullable(eventStateAggregate.hasNonNull("orgEnhetsType") ? OrgEnhetsType.valueOf(eventStateAggregate.get("orgEnhetsType").asText()) : null);

        // Agresso data
        var additionalData = Optional.ofNullable(eventStateAggregate.hasNonNull("additionalData") ? eventStateAggregate.get("additionalData") : null);
        Optional<String> uniqueAgressoId = Optional.ofNullable(additionalData.isPresent() && additionalData.get().hasNonNull("uniqueAgressoId") ? additionalData.get().get("uniqueAgressoId").asText() : null);
        Optional<String> agressoId = Optional.ofNullable(additionalData.isPresent() && additionalData.get().hasNonNull("agressoId") ? additionalData.get().get("agressoId").asText() : null);
        Optional<String> agressoOrgNivaa = Optional.ofNullable(additionalData.isPresent() && additionalData.get().hasNonNull("agressoOrgNivaa") ? additionalData.get().get("agressoOrgNivaa").asText() : null);
        Optional<String> agressoOrgenhetType = Optional.ofNullable(additionalData.isPresent() && additionalData.get().hasNonNull("agressoOrgenhetType") ? additionalData.get().get("agressoOrgenhetType").asText() : null);

        try {
            return new OrgEnhetState(
                fid,
                eventStateAggregate.get("id").asText(),
                eventStateAggregate.get("navn").asText(),
                LocalDate.parse(eventStateAggregate.get("gyldigFom").asText()),
                Optional.ofNullable(eventStateAggregate.hasNonNull("gyldigTom") ? LocalDate.parse(eventStateAggregate.get("gyldigTom").asText()) : null),
                overordnetOrgEnhetFid,
                lederFid,
                orgEnhetsType,
                orgEnhetsKategori,
                uniqueAgressoId,
                agressoId,
                agressoOrgNivaa,
                agressoOrgenhetType
            );
        } catch (Exception e){
            log.error("Error mapping to OrgEnhetState", e);
            throw e;
        }
    }
}
