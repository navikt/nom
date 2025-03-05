package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser.v1.Orgtilknytning;
import no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser.v1.RessursOpprettetEvent;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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


        // Aggregerer ressurser siden det kan være RessursOpprettet-events som ikke har alle nødvendige felt
        KTable<String, ObjectNode> ressursAggregateKTable = streamsBuilder
                .stream(appProperties.ressursEvtsrcTopic(), Consumed.with(stringSerde, ressursEventSerde))
                .filter((k, event) -> !event.getFid().startsWith("FID_MANUELL")) // Ikke behandle manuelle test ressurser/orgtilknytninger
                .groupBy((k, event) -> event.getFid(), Grouped.with(stringSerde, ressursEventSerde))
                .aggregate(
                        objectMapper::createObjectNode,
                        (String key, RessursEvent event, ObjectNode ressursAggregate) -> {
                            ressursAggregate.put("fid", event.getFid());

                            if (event.getAdditionalData() instanceof RessursOpprettetEventV1.RessursOpprettetAdditionalData additionalData) {
                                if (additionalData.navident().isPresent()) {
                                    ressursAggregate.put("navident", additionalData.navident().get());
                                }
                                if (additionalData.startdato().isPresent()) {
                                    ressursAggregate.put("startdato", String.valueOf(additionalData.startdato().get()));
                                }
                            }

                            ressursAggregate.setAll(objectMapper.convertValue(event.getData(), ObjectNode.class));

                            if (ressursHasRequiredFields(ressursAggregate)) {

                                if (ressursAggregate.get("ressursReadyForJoin") == null) {
                                    ressursAggregate.put("ressursReadyForJoin", "true");
                                } else {
                                    ressursAggregate.put("ressursReadyForJoin", "false");
                                }
                            }

                            return ressursAggregate;
                        },
                        Materialized.with(stringSerde, jsonSerde)
                );

        // Snur til å være pr. navident og kun ha med ressurser hvor nødvendige felter har fått verdi
        KTable<String, ObjectNode> ressursPerNavident = ressursAggregateKTable.toStream()
                .filter((k, event) -> isFieldSetAndTrue(event, "ressursReadyForJoin"))
                .selectKey((key, otJson) -> otJson.get("navident").asText())
                .groupByKey(Grouped.with(stringSerde, jsonSerde))
                .reduce((previousValue, newValue) -> newValue);

        ressursPerNavident
                .toStream()
                .print(Printed.<String, ObjectNode>toSysOut().withLabel("ressursPerNavident"));
        

        // Aggregerer orgtilknytninger
        KTable<String, ObjectNode> orgtilknytningAggregateKTable = streamsBuilder
                .stream(appProperties.orgtilknytningEvtsrcTopic(), Consumed.with(stringSerde, orgtilknytningEventSerde))
                .filter((k, event) -> !event.getFid().startsWith("FID_MANUELL")) // Ikke behandle manuelle test ressurser/orgtilknytninger
                .groupBy((String k, OrgtilknytningEvent event) -> event.getFid(), Grouped.with(stringSerde, orgtilknytningEventSerde))
                .aggregate(
                        objectMapper::createObjectNode,
                        (String key, OrgtilknytningEvent newEvent, ObjectNode orgtilknytningAggregate) -> {
                            orgtilknytningAggregate.put("fid", newEvent.getFid());

                            // Hvis DagligOppfolgingEndretEvent mottas og setter DOL så skal join med ressurs trigges
                            // For andre eventer hvor DOL er satt skal join ikke trigges
                            orgtilknytningAggregate.setAll(objectMapper.convertValue(newEvent.getData(), ObjectNode.class));

                            // Hvis DagligOppfolgingEndretEvent mottas og setter DOL så kan OTen gå videre til neste steg
                            // For påfølgende eventer hvor DOL er satt skal join ikke trigges
                            if (orgtilknytningHasRequiredFields(orgtilknytningAggregate)) {
                                var existingStatus = orgtilknytningAggregate.get("otReadyForJoin");
                                if (existingStatus != null && existingStatus.asText().equals("true")) {
                                    orgtilknytningAggregate.put("otReadyForJoin", "false");
                                } else {
                                    orgtilknytningAggregate.put("otReadyForJoin", "true");
                                }
                            }

                            return orgtilknytningAggregate;
                        },
                        Materialized.with(stringSerde, jsonSerde)
                );

        // Ta med OTer som er DOL, snu til å være per navident og merke første OT for denne navidenten for sending 
        KTable<String, ObjectNode> orgtilknytningMappedToNavident = orgtilknytningAggregateKTable.toStream()
                .filter((key, otJson) -> isFieldSetAndTrue(otJson, "otReadyForJoin"))
                .selectKey((key, otJson) -> otJson.get("navident").asText())
                .groupByKey(Grouped.with(stringSerde, jsonSerde))
                .aggregate(
                        objectMapper::createObjectNode,
                        (String key, ObjectNode newEvent, ObjectNode aggregate) -> {
                            aggregate.setAll(newEvent);

                            // Merke første OT for navidenten og ellers fjerne merket, slik at det bare sendes en melding pr. navident
                            if (aggregate.get("sendMessage") == null ) {
                                aggregate.put("sendMessage", "true");
                            } else {
                                aggregate.put("sendMessage", "false");
                            }

                            return aggregate;
                        },
                        Materialized.with(stringSerde, jsonSerde)
                );

        orgtilknytningMappedToNavident
                .toStream()
                .print(Printed.<String, ObjectNode>toSysOut().withLabel("orgtilknytningMappedToNavident"));

        // Kun ta med orgtilkyntinger som skal joines 
        KTable<String, ObjectNode> forsteDOLOrgtilknytningPrNavident = orgtilknytningMappedToNavident.toStream()
                .filter((key, otJson) -> isFieldSetAndTrue(otJson, "sendMessage"))
                .toTable();

        forsteDOLOrgtilknytningPrNavident
                .toStream()
                .print(Printed.<String, ObjectNode>toSysOut().withLabel("forsteDOLOrgtilknytningPrNavident"));


        // Joiner på navident
        ressursPerNavident.join(
                        forsteDOLOrgtilknytningPrNavident,
                        this::toRessursOpprettetEvent,
                        Materialized.with(stringSerde, ressursOpprettetEventSerde))
                .toStream()
                .to(appProperties.ressursHendelseTopic(), Produced.with(stringSerde, ressursOpprettetEventSerde));
    }

    private boolean orgtilknytningHasRequiredFields(ObjectNode orgtilknytningAggregate) {
        return orgtilknytningAggregate.get("dagligOppfolging") != null &&
                orgtilknytningAggregate.get("dagligOppfolging").asText().equals("true");
    }

    private boolean ressursHasRequiredFields(ObjectNode ressursAggregate) {
        return ressursAggregate.get("navident") != null
                && !ressursAggregate.get("navident").textValue().isEmpty()
                && ressursAggregate.get("startdato") != null
                && !ressursAggregate.get("startdato").textValue().isEmpty();
    }

    private static boolean isFieldSetAndTrue(ObjectNode value, String fieldName) {
        JsonNode dolNode = value.get(fieldName);
        return dolNode != null && dolNode.asText().equals("true");
    }

    private RessursOpprettetEvent toRessursOpprettetEvent(
            ObjectNode ressursAggregate,
            ObjectNode orgtilknytningAggregate
    ) {
        try {
            final Orgtilknytning orgtilknytning = Orgtilknytning.builder()
                    .fid(orgtilknytningAggregate.get("fid").asText())
                    .orgEnhetId(orgtilknytningAggregate.get("orgEnhetId").asText())
                    .gyldigFom(orgtilknytningAggregate.hasNonNull("gyldigFom") ?
                            LocalDate.parse(orgtilknytningAggregate.get("gyldigFom").asText()) : null)
                    .gyldigTom(Optional.ofNullable(orgtilknytningAggregate.hasNonNull("gyldigTom") ?
                            LocalDate.parse(orgtilknytningAggregate.get("gyldigTom").asText()) : null))
                    .dagligOppfolging(orgtilknytningAggregate.get("dagligOppfolging").asBoolean())
                    .build();

            final JsonNode visningsnavn = ressursAggregate.get("visningsnavn");
            return RessursOpprettetEvent.builder()
                    .fid(ressursAggregate.get("fid").asText())
                    .navident(ressursAggregate.get("navident").asText())
                    .visningsnavn(new Visningsnavn(
                            visningsnavn.get("fornavn").asText(),
                            Optional.ofNullable(visningsnavn.hasNonNull("mellomnavn") ? visningsnavn.get("mellomnavn").asText() : null),
                            visningsnavn.get("etternavn").asText()
                    ))
                    .startdato(ressursAggregate.hasNonNull("startdato") ? LocalDate.parse(ressursAggregate.get("startdato").asText()) : null)
                    .sluttdato(Optional.ofNullable(ressursAggregate.hasNonNull("sluttdato") ? LocalDate.parse(ressursAggregate.get("sluttdato").asText()) : null))
                    .sektor(ressursAggregate.hasNonNull("sektor") ? Sektor.valueOf(ressursAggregate.get("sektor").asText()) : null)
                    .orgtilknytning(orgtilknytning)
                    .build();
        } catch (Exception e) {
            log.error("Error mapping '{}' and '{}' to RessursOpprettetEvent", ressursAggregate, orgtilknytningAggregate, e);
            throw e;
        }
    }

}
