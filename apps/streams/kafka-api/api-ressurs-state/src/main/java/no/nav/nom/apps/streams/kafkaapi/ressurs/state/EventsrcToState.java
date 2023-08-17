package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v2.Orgtilknytning;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v2.RessursState;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFjernetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
class EventsrcToState {
    private final static Logger log = LoggerFactory.getLogger(EventsrcToState.class);

    public EventsrcToState(
            @Value("${app.kafka.ressurs-evtsrc-topic}") String ressursEvtsrcTopic,
            @Value("${app.kafka.orgtilknytning-evtsrc-topic}") String orgtilknytningEvtsrcTopic,
            @Value("${app.kafka.state-topic.v2}") String stateTopic,
            ObjectMapper objectMapper,
            JsonSerde<Object> objectSerde,
            StreamsBuilder streamsBuilder
    ){
        final var stringSerde = Serdes.String();
        final var jsonSerde = objectSerde.copyWithType(ObjectNode.class);
        final var ressursEventSerde = objectSerde.copyWithType(RessursEvent.class);
        final var orgtilknytningEventSerde = objectSerde.copyWithType(OrgtilknytningEvent.class);
        final JavaType ressursMapEntryType = objectMapper.getTypeFactory().constructParametricType(Map.Entry.class, String.class, RessursState.class);
        final JavaType orgtilknytningMapType = objectMapper.getTypeFactory().constructParametricType(Map.class, String.class, Orgtilknytning.class);

        // Aggreger ressurs evtsrc til en Json med sum av verdier, returner per navident
        var ressursAggregateKTable = streamsBuilder.stream(ressursEvtsrcTopic, Consumed.with(stringSerde, ressursEventSerde))
            .groupBy((k, v) -> v.getFid(), Grouped.with(stringSerde, ressursEventSerde))
            .aggregate(
                objectMapper::createObjectNode,
                (key, event, state) -> {
                    if(event.getAdditionalData() instanceof RessursOpprettetEventV1.RessursOpprettetAdditionalData data){
                        if(data.navident().isPresent()){
                            state.put("navident", data.navident().get());
                        }
                        if(data.startdato().isPresent()){
                            state.put("startdato", String.valueOf(data.startdato().get()));
                        }
                    }
                    state.setAll(objectMapper.convertValue(event.getData(), ObjectNode.class));
                    state.put("fid", event.getFid()); // Keep fid - Ønsker å benytte denne som key på output
                    return state;
                },
                Materialized.with(stringSerde, jsonSerde)
            );

        // Snu denne til å være per navident
        var ressursPerNavident = ressursAggregateKTable.toStream()
            .selectKey( (fid, ressursJson) -> ressursJson.get("navident").asText())
            .groupByKey(Grouped.with(stringSerde, jsonSerde))
            .reduce((oldRessurs, newRessurs) -> newRessurs,  // Keep only newest ressurs - Actually - there will alwasy be one-to-one fid<->navident, so only one will ever emerge
                Materialized.with(stringSerde, jsonSerde)
            );

        // Aggreger orgtilknytning evtsrc til en Json som er sum per tilknytnig, returner per fid
        var orgtilknytningAggregateKTable = streamsBuilder.stream(orgtilknytningEvtsrcTopic, Consumed.with(stringSerde, orgtilknytningEventSerde))
            .groupBy((k, v) -> v.getFid(), Grouped.with(stringSerde, orgtilknytningEventSerde))
            .aggregate(
                objectMapper::createObjectNode,
                (key, event, state) -> {
                    if(event instanceof OrgtilknytningFjernetEventV1) {
                        state.put("TOMBSTONE", true);
                        //                           return null; // Create TOMBSTONE - should skip mapValues and get forwarded directly to output stream. -
                        // (See https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Stream+Usage+Patterns#KafkaStreamUsagePatterns-Howtomanagethesizeofstatestoresusingtombstones?)
                        //                              NOPE ! Does not work. If multiple input event at once, it works, but null event alone will be 'reduced' away
                    } else {
                        state.setAll(objectMapper.convertValue(event.getData(), ObjectNode.class));
                    }
                    state.put("fid", event.getFid());
                    return state;
                },
                Materialized.with(stringSerde, jsonSerde)
            );

//      Map orgtilknytningene i en Map per navident
        KTable<String, Map<String, Orgtilknytning>> orgtilknytningMappedToNavident = orgtilknytningAggregateKTable.toStream()
            .selectKey( (key, otJson) -> otJson.get("navident").asText())
            .groupByKey(Grouped.with(stringSerde, jsonSerde))
            .aggregate(
                HashMap::new,
                (navident, otJson, map) -> {
                    var fid = otJson.get("fid").asText();
                    if (otJson.has("TOMBSTONE")) {
                        map.remove(fid);
                    } else {
                        map.put(otJson.get("fid").asText(), toOrgtilknytning(otJson));
                    }
                    return map;
                },
                Materialized.with(stringSerde, objectSerde.copyWithType(orgtilknytningMapType))
            );

//      Left Join ressurs med orgtilknytninger, re-map til ressurs.fid og send to output
        ressursPerNavident.leftJoin(
                orgtilknytningMappedToNavident,
                this::toRessursState,
                Materialized.with(stringSerde, objectSerde.copyWithType(ressursMapEntryType))
            )
            .toStream()
            .selectKey( (navident, ressursEntry) -> ressursEntry.getKey() )
            .mapValues(Map.Entry::getValue)
            .to(stateTopic, Produced.with(stringSerde, objectSerde.copyWithType(RessursState.class)));
    }

    public Map.Entry<String, RessursState> toRessursState(JsonNode eventStateAggregate, Map<String, Orgtilknytning> orgtilknytningerMap){
        try {
            final var visningsnavn = eventStateAggregate.get("visningsnavn");
            final var orgtilknytninger = Optional.ofNullable(orgtilknytningerMap == null ? null : orgtilknytningerMap.values().stream().toList());
            return new AbstractMap.SimpleEntry<>(
                eventStateAggregate.get("fid").asText(), // Plukk ut igjen Fid til key
                new RessursState(
                    eventStateAggregate.get("personident").asText(),
                    eventStateAggregate.get("navident").asText(),
                    new Visningsnavn(
                            visningsnavn.get("fornavn").asText(),
                            Optional.ofNullable(visningsnavn.hasNonNull("mellomnavn") ? visningsnavn.get("mellomnavn").asText() : null),
                            visningsnavn.get("etternavn").asText()
                    ),
                    Sektor.valueOf(eventStateAggregate.get("sektor").asText()),
                    orgtilknytninger,
                    Optional.ofNullable(eventStateAggregate.hasNonNull("startdato") ? LocalDate.parse(eventStateAggregate.get("startdato").asText()) : null),
                    Optional.ofNullable(eventStateAggregate.hasNonNull("sluttdato") ? LocalDate.parse(eventStateAggregate.get("sluttdato").asText()) : null)
                )
            );
        } catch (Exception e){
            log.error("Error mapping to RessursState", e);
            throw e;
        }
    }

    public Orgtilknytning toOrgtilknytning(JsonNode otJson){
        try {
            return new Orgtilknytning(
                otJson.get("fid").asText(),
                otJson.get("orgEnhetId").asText(),
                LocalDate.parse(otJson.get("gyldigFom").asText()),
                Optional.ofNullable(otJson.hasNonNull("gyldigTom") ? LocalDate.parse(otJson.get("gyldigTom").asText()) : null),
                otJson.hasNonNull("dagligOppfolging") && otJson.get("dagligOppfolging").asBoolean()
            );
        } catch (Exception e){
            log.error("Error mapping to Orgtilknytning", e);
            throw e;
        }
    }
}
