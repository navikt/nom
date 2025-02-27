package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser.v1.Orgtilknytning;
import no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser.v1.RessursOpprettetEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1.RessursOpprettetAdditionalData;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1.RessursOpprettetEventData;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class EventsourceToExternalEvents {

    private final static Logger log = LoggerFactory.getLogger(EventsourceToExternalEvents.class);


    public EventsourceToExternalEvents(
            ObjectMapper objectMapper,
            JsonSerde<Object> objectSerde,
            StreamsBuilder streamsBuilder,
            AppProperties appProperties
    ) {
        final var stringSerde = Serdes.String();
        final var jsonSerde = objectSerde.copyWithType(ObjectNode.class);


        // Input-meldinger
        final var ressursEventSerde = objectSerde.copyWithType(RessursEvent.class);
        final var orgtilknytningEventSerde = objectSerde.copyWithType(OrgtilknytningEvent.class);

        // Output-meldinger
        final var ressursOpprettetEventSerde = objectSerde.copyWithType(RessursOpprettetEvent.class);

        final JavaType orgtilknytningMapType = objectMapper.getTypeFactory().constructParametricType(Map.class, String.class, Orgtilknytning.class);


        KTable<String, RessursEvent> ressursPerNavident = streamsBuilder
                .stream(appProperties.ressursEvtsrcTopic(), Consumed.with(stringSerde, ressursEventSerde))
                .filter((k, event) -> event instanceof RessursOpprettetEventV1)
                .filter((k, event) -> !event.getFid().startsWith("FID_MANUELL")) // Ikke behandle manuelle test ressurser/orgtilknytninger
                .filter((k, event) -> hasNavident(event)) // Ikke behandle eventer hvor navident mangler
                .map((fid, ressursEvent) -> {
                    RessursOpprettetAdditionalData ressursOpprettetAdditionalData = (RessursOpprettetAdditionalData) ressursEvent.getAdditionalData();

                    //noinspection OptionalGetWithoutIsPresent
                    return KeyValue.pair(ressursOpprettetAdditionalData.navident().get(), ressursEvent);
                })
                .groupByKey(Grouped.with(stringSerde, ressursEventSerde))
                .reduce((previousValue, newValue) -> newValue);
        
        // Aggregerer orgtilknytninger
        KTable<String, ObjectNode> orgtilknytningAggregateKTable = streamsBuilder
                .stream(appProperties.orgtilknytningEvtsrcTopic(), Consumed.with(stringSerde, orgtilknytningEventSerde))
                .filter((k, event) -> !event.getFid().startsWith("FID_MANUELL")) // Ikke behandle manuelle test ressurser/orgtilknytninger
                .groupBy((String k, OrgtilknytningEvent event) -> event.getFid(), Grouped.with(stringSerde, orgtilknytningEventSerde))
                .aggregate(
                        objectMapper::createObjectNode,
                        (String key, OrgtilknytningEvent newEvent, ObjectNode orgtilknytningAggregate) -> {
                            log.debug("OrgtilknytningAggregate key: {} before: {}", key, orgtilknytningAggregate);
                            orgtilknytningAggregate.setAll(objectMapper.convertValue(newEvent.getData(), ObjectNode.class));
                            orgtilknytningAggregate.put("fid", newEvent.getFid());
                            log.debug("OrgtilknytningAggregate key: {} after: {}", key, orgtilknytningAggregate);
                            return orgtilknytningAggregate;
                        },
                        Materialized.with(stringSerde, jsonSerde)
                );
        
        // Snu til å være per navident og kun returnere OTer som er DOL
        KTable<String, Map<String, Orgtilknytning>> orgtilknytningMappedToNavident = orgtilknytningAggregateKTable.toStream()
                .filter((key, value) -> isDagligOppfolging(value))
                .selectKey((key, otJson) -> otJson.get("navident").asText())
                .groupByKey(Grouped.with(stringSerde, jsonSerde))
                .aggregate(
                        HashMap::new,
                        (String navident, ObjectNode otJson, Map<String, Orgtilknytning> map) -> {
                            map.put(otJson.get("fid").asText(), toOrgtilknytning(otJson));
                            return map;
                        },
                        Materialized.with(stringSerde, objectSerde.copyWithType(orgtilknytningMapType))
                );

        // Joiner på navident
        ressursPerNavident.join(
                        orgtilknytningMappedToNavident,
                        this::toRessursOpprettetEvent,
                        Materialized.with(stringSerde, ressursOpprettetEventSerde))
                .toStream()
                .to(appProperties.ressursHendelseTopic(), Produced.with(stringSerde, ressursOpprettetEventSerde));

    }

    private static boolean hasNavident(RessursEvent ressursEvent) {
        RessursOpprettetAdditionalData ressursOpprettetAdditionalData =
                (RessursOpprettetAdditionalData) ressursEvent.getAdditionalData();

        if (ressursOpprettetAdditionalData.navident().isPresent()) {
            return true;
        } else {
            log.warn("RessursEvent mangler navident: {}", ressursEvent);
            return false;
        }
    }

    private static boolean isDagligOppfolging(ObjectNode value) {
        JsonNode dolNode = value.get("dagligOppfolging");
        if (dolNode != null) {
            return dolNode.asBoolean();
        } else {
            return false;
        }
    }

    private RessursOpprettetEvent toRessursOpprettetEvent(RessursEvent ressursEvent, Map<String, Orgtilknytning> orgtilknytningMap) {
        RessursOpprettetEventData eventData = (RessursOpprettetEventData) ressursEvent.getData();
        var additionalData = (RessursOpprettetAdditionalData) ressursEvent.getAdditionalData();

        log.debug("Kombinerer RessursEvent:\n {} \nmed Orgtilknytninger:\n {}", ressursEvent, orgtilknytningMap);

        final Orgtilknytning orgtilknytning = orgtilknytningMap.values().stream().findFirst().orElse(null);

        return RessursOpprettetEvent.builder()
                .fid(ressursEvent.getFid())
                .navident(additionalData.navident().isPresent() ? additionalData.navident().get() : null)
                .visningsnavn(eventData.visningsnavn())
                .startdato(additionalData.startdato().isPresent() ? additionalData.startdato().get() : null)
                .sektor(eventData.sektor())
                .orgtilknytning(orgtilknytning)
                .build();
    }

    private Orgtilknytning toOrgtilknytning(JsonNode otJson) {
        try {
            return Orgtilknytning.builder()
                    .fid(otJson.get("fid").asText())
                    .orgEnhetId(otJson.get("orgEnhetId").asText())
                    .gyldigFom(LocalDate.parse(otJson.get("gyldigFom").asText()))
                    .gyldigTom(Optional.ofNullable(otJson.hasNonNull("gyldigTom") ?
                            LocalDate.parse(otJson.get("gyldigTom").asText()) : null))
                    .dagligOppfolging(otJson.hasNonNull("dagligOppfolging") &&
                            otJson.get("dagligOppfolging").asBoolean())
                    .build();
        } catch (Exception e) {
            log.error("Error mapping '{}' to Orgtilknytning", otJson, e);
            throw e;
        }
    }

}
