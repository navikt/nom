package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser.v1.Orgtilknytning;
import no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser.v1.RessursOpprettetEvent;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.*;
import org.apache.kafka.common.serialization.Serdes;
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


        // Aggregerer ressurser siden det kan være RessursOpprettet-events som ikke har alle nødvendige felt
        KTable<String, ObjectNode> ressursAggregateKTable = streamsBuilder
                .stream(appProperties.ressursEvtsrcTopic(), Consumed.with(stringSerde, ressursEventSerde))
                .filter((k, event) -> !event.getFid().startsWith("FID_MANUELL")) // Ikke behandle manuelle test ressurser/orgtilknytninger
                .groupBy((k, event) -> event.getFid(), Grouped.with(stringSerde, ressursEventSerde))
                .aggregate(
                        objectMapper::createObjectNode,
                        (String key, RessursEvent ressursEvent, ObjectNode ressursAggregate) -> {
                            ressursAggregate.put("fid", ressursEvent.getFid());

                            if (ressursEvent.getAdditionalData() instanceof RessursOpprettetEventV1.RessursOpprettetAdditionalData additionalData) {
                                if (additionalData.navident().isPresent()) {
                                    ressursAggregate.put("navident", additionalData.navident().get());
                                }
                                if (additionalData.startdato().isPresent()) {
                                    ressursAggregate.put("startdato", String.valueOf(additionalData.startdato().get()));
                                }
                            }

                            ressursAggregate.setAll(objectMapper.convertValue(ressursEvent.getData(), ObjectNode.class));

                            if (hasRequiredFields(ressursAggregate)) {
                                ressursAggregate.put("requiredFieldsPresent", "true");
                            }

                            return ressursAggregate;
                        },
                        Materialized.with(stringSerde, jsonSerde)
                );

        // Snur til å være pr. navident og kun ha med ressurser hvor nødvendige felter har fått verdi
        KTable<String, ObjectNode> ressursPerNavident = ressursAggregateKTable.toStream()
                .filter((k, event) -> hasRequiredFieldsForRessurs(event))
                .selectKey((key, otJson) -> otJson.get("navident").asText())
                .groupByKey(Grouped.with(stringSerde, jsonSerde))
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

                            // Hvis DagligOppfolgingEndretEvent mottas og setter DOL så skal join med ressurs trigges
                            // For andre eventer hvor DOL er satt skal join ikke trigges
                            if (newEvent instanceof OrgtilknytningDagligOppfolgingEndretEventV1 &&
                                    ((OrgtilknytningDagligOppfolgingEndretEventV1) newEvent).getOrgtilknytningDagligOppfolgingEndretEventData().dagligOppfolging()) {
                                orgtilknytningAggregate.put("newDOLReceived", "true");
                            } else if (orgtilknytningAggregate.get("newDOLReceived") != null &&
                                    orgtilknytningAggregate.get("newDOLReceived").asText().equals("true")) {
                                orgtilknytningAggregate.put("newDOLReceived", "false");
                            }

                            orgtilknytningAggregate.setAll(objectMapper.convertValue(newEvent.getData(), ObjectNode.class));
                            orgtilknytningAggregate.put("fid", newEvent.getFid());

                            log.debug("OrgtilknytningAggregate key: {} after: {}", key, orgtilknytningAggregate);
                            return orgtilknytningAggregate;
                        },
                        Materialized.with(stringSerde, jsonSerde)
                );

        // Snu til å være per navident og kun returnere OTer som er DOL
        KTable<String, Map<String, Orgtilknytning>> orgtilknytningMappedToNavident = orgtilknytningAggregateKTable.toStream()
                .filter((key, value) -> isNewDagligOppfolging(value))
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

    private boolean hasRequiredFields(ObjectNode ressursAggregate) {
        return ressursAggregate.get("navident") != null
                && !ressursAggregate.get("navident").textValue().isEmpty()
                && ressursAggregate.get("startdato") != null
                && !ressursAggregate.get("startdato").textValue().isEmpty();
    }

    private static boolean isNewDagligOppfolging(ObjectNode value) {
        JsonNode dolNode = value.get("newDOLReceived");
        return dolNode != null && dolNode.asText().equals("true");
    }

    private static boolean hasRequiredFieldsForRessurs(ObjectNode value) {
        JsonNode dolNode = value.get("requiredFieldsPresent");
        return dolNode != null && dolNode.asText().equals("true");
    }

    private RessursOpprettetEvent toRessursOpprettetEvent(
            JsonNode eventAggregate,
            Map<String, Orgtilknytning> orgtilknytningMap
    ) {
        log.debug("Kombinerer RessursEvent med Orgtilknytning:\n {}\n {}", eventAggregate, orgtilknytningMap);

        try {
            final JsonNode visningsnavn = eventAggregate.get("visningsnavn");
            final Orgtilknytning orgtilknytning = orgtilknytningMap.values().stream().findFirst().orElse(null);

            return RessursOpprettetEvent.builder()
                    .fid(eventAggregate.get("fid").asText())
                    .navident(eventAggregate.get("navident").asText())
                    .visningsnavn(new Visningsnavn(
                            visningsnavn.get("fornavn").asText(),
                            Optional.ofNullable(visningsnavn.hasNonNull("mellomnavn") ? visningsnavn.get("mellomnavn").asText() : null),
                            visningsnavn.get("etternavn").asText()
                    ))
                    .startdato(eventAggregate.hasNonNull("startdato") ? LocalDate.parse(eventAggregate.get("startdato").asText()) : null)
                    .sluttdato(Optional.ofNullable(eventAggregate.hasNonNull("sluttdato") ? LocalDate.parse(eventAggregate.get("sluttdato").asText()) : null))
                    .sektor(eventAggregate.hasNonNull("sektor") ? Sektor.valueOf(eventAggregate.get("sektor").asText()) : null)
                    .orgtilknytning(orgtilknytning)
                    .build();
        } catch (Exception e) {
            log.error("Error mapping '{}' to RessursOpprettetEvent", eventAggregate, e);
            throw e;
        }
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
