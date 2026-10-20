package no.nav.nom.apps.streams.internal.orgenhet.eventsrcToState;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetKorrigertEventV1;
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
import org.springframework.kafka.support.serializer.JacksonJsonSerde;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.util.Optional;

@Component
class EventsrcToState {
    private final static Logger log = LoggerFactory.getLogger(EventsrcToState.class);

    public EventsrcToState(
            @Value("${app.kafka.eventsourceTopic}") String eventsourceTopic,
            @Value("${app.kafka.stateTopic}") String stateTopic,
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
                            } else if (event.getAdditionalData() instanceof OrgEnhetKorrigertEventV1.OrgEnhetKorrigertAdditionalAgressoData data){
                                state.set("additionalData", jsonMapper.convertValue(data, ObjectNode.class));
                            }
                            ObjectNode eventData = jsonMapper.convertValue(event.getData(), ObjectNode.class);
                            if (eventData.has("id") && (eventData.get("id").isNull() || eventData.get("id").asString().isBlank())) {
                                eventData.remove("id");
                            }
                            state.setAll(eventData);
                            return state;
                        },
                        Materialized.with(stringSerde, objectSerde.copyWithType(ObjectNode.class))
                )
                .mapValues(this::toOrgEnhetState)
                .toStream().to(stateTopic, Produced.with(stringSerde, objectSerde.copyWithType(OrgEnhetState.class)));
    }

    public OrgEnhetState toOrgEnhetState(String fid, JsonNode eventStateAggregate){
        if (!eventStateAggregate.hasNonNull("id")) {
            throw new IllegalStateException("OrgEnhet state is missing required id for fid " + fid);
        }

        String overordnetOrgEnhetFid = eventStateAggregate.hasNonNull("overordnetOrgEnhetFid") ? eventStateAggregate.get("overordnetOrgEnhetFid").asString() : null;
        String lederFid = eventStateAggregate.hasNonNull("lederRessursFid") ? eventStateAggregate.get("lederRessursFid").asString() : null;

        NomNivaa orgEnhetsKategori = eventStateAggregate.hasNonNull("nomNivaa") ? NomNivaa.valueOf(eventStateAggregate.get("nomNivaa").asString()) : null;
        OrgEnhetsType orgEnhetsType =  eventStateAggregate.hasNonNull("orgEnhetsType") ? OrgEnhetsType.valueOf(eventStateAggregate.get("orgEnhetsType").asString()) : null;

        String tilgangsenhetId = eventStateAggregate.hasNonNull("tilgangsenhetId") ? eventStateAggregate.get("tilgangsenhetId").asString() : null;

        // Agresso data
        var additionalData = Optional.ofNullable(eventStateAggregate.hasNonNull("additionalData") ? eventStateAggregate.get("additionalData") : null);
        String uniqueAgressoId = additionalData.isPresent() && additionalData.get().hasNonNull("uniqueAgressoId") ? additionalData.get().get("uniqueAgressoId").asString() : null;
        String agressoId = additionalData.isPresent() && additionalData.get().hasNonNull("agressoId") ? additionalData.get().get("agressoId").asString() : null;
        String agressoOrgNivaa = additionalData.isPresent() && additionalData.get().hasNonNull("agressoOrgNivaa") ? additionalData.get().get("agressoOrgNivaa").asString() : null;
        String agressoOrgenhetType = additionalData.isPresent() && additionalData.get().hasNonNull("agressoOrgenhetType") ? additionalData.get().get("agressoOrgenhetType").asString() : null;

        try {
            return new OrgEnhetState(
                fid,
                eventStateAggregate.get("id").asString(),
                eventStateAggregate.get("navn").asString(),
                LocalDate.parse(eventStateAggregate.get("gyldigFom").asString()),
                eventStateAggregate.hasNonNull("gyldigTom") ? LocalDate.parse(eventStateAggregate.get("gyldigTom").asString()) : null,
                overordnetOrgEnhetFid,
                lederFid,
                orgEnhetsType,
                orgEnhetsKategori,
                uniqueAgressoId,
                agressoId,
                agressoOrgNivaa,
                agressoOrgenhetType,
                tilgangsenhetId
            );
        } catch (Exception e){
            log.error("Error mapping to OrgEnhetState", e);
            throw e;
        }
    }
}
