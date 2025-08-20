package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser;

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
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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
        ressursOpprettetEventSerde.serializer().noTypeInfo(); // Ikke inkludere type i eksterne meldinger


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
                                if (additionalData.navident() != null) {
                                    ressursAggregate.put("navident", additionalData.navident());
                                }
                                if (additionalData.startdato() != null) {
                                    ressursAggregate.put("startdato", String.valueOf(additionalData.startdato()));
                                }
                            }

                            ressursAggregate.setAll(objectMapper.convertValue(event.getData(), ObjectNode.class));

                            // Merke ressursen når den har nødvendige felter
                            if (ressursHasRequiredFields(ressursAggregate)) {
                                ressursAggregate.put("ressursReadyForJoin", "true");
                            }

                            return ressursAggregate;
                        },
                        Materialized.with(stringSerde, jsonSerde)
                );

        ressursAggregateKTable
            .toStream()
            .print(Printed.<String, ObjectNode>toSysOut().withLabel("ressursAggregateKTable"));

        // Snur til å være pr. navident og kun ha med ressurser hvor nødvendige felter har fått verdi
        KTable<String, ObjectNode> ressursPerNavident = ressursAggregateKTable.toStream()
            .filter((k, event) -> isFieldSetAndTrue(event, "ressursReadyForJoin"))
            .selectKey((key, otJson) -> otJson.get("navident").asText())
            .groupByKey(Grouped.with(stringSerde, jsonSerde))
            .aggregate(
                objectMapper::createObjectNode,
                (String key, ObjectNode newEvent, ObjectNode aggregate) -> {
                    log.debug("Aggregate before: {}", aggregate);

                    aggregate.setAll(newEvent);

                    // Merke navidenten første gang og ellers fjerne merket, slik at det bare sendes en melding pr. navident
                    if (aggregate.get("sendMessage") == null) {
                        aggregate.put("sendMessage", "true");
                    } else {
                        aggregate.put("sendMessage", "false");
                    }

                    log.debug("Aggregate after: {}", aggregate);
                    return aggregate;
                },
                Materialized.with(stringSerde, jsonSerde)
            );


        ressursPerNavident
                .toStream()
                .print(Printed.<String, ObjectNode>toSysOut().withLabel("ressursPerNavident"));

        // Kun ta med ressurser som det skal sendes melding for
        KTable<String, ObjectNode> ressurserKlarTilSending = ressursPerNavident.toStream()
            .filter((key, rJson) -> isFieldSetAndTrue(rJson, "sendMessage"))
            .toTable();

        ressurserKlarTilSending
            .toStream()
            .print(Printed.<String, ObjectNode>toSysOut().withLabel("ressurserKlarTilSending"));

        // Aggregerer orgtilknytninger
        KTable<String, ObjectNode> orgtilknytningAggregateKTable = streamsBuilder
                .stream(appProperties.orgtilknytningEvtsrcTopic(), Consumed.with(stringSerde, orgtilknytningEventSerde))
                .filter((k, event) -> !event.getFid().startsWith("FID_MANUELL")) // Ikke behandle manuelle test ressurser/orgtilknytninger
                .groupBy((String k, OrgtilknytningEvent event) -> event.getFid(), Grouped.with(stringSerde, orgtilknytningEventSerde))
                .aggregate(
                        objectMapper::createObjectNode,
                        (String key, OrgtilknytningEvent newEvent, ObjectNode orgtilknytningAggregate) -> {
                            orgtilknytningAggregate.put("fid", newEvent.getFid());

                            orgtilknytningAggregate.setAll(objectMapper.convertValue(newEvent.getData(), ObjectNode.class));

                            // Hvis DagligOppfolgingEndretEvent mottas og setter DOL så kan OTen gå videre til neste steg
                            // For påfølgende eventer hvor DOL er satt skal join ikke trigges
                            if (orgtilknytningHasRequiredFields(orgtilknytningAggregate)) {
                                JsonNode existingStatus = orgtilknytningAggregate.get("otReadyForJoin");
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

        orgtilknytningAggregateKTable
                .toStream()
                .print(Printed.<String, ObjectNode>toSysOut().withLabel("orgtilknytningAggregateKTable"));


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
        ressurserKlarTilSending.join(
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
        JsonNode navidentNode = ressursAggregate.get("navident");
        JsonNode startdatoNode = ressursAggregate.get("startdato");

        return navidentNode != null && navidentNode.textValue() != null && !navidentNode.textValue().isEmpty()
                && startdatoNode != null && startdatoNode.textValue() != null && !startdatoNode.textValue().isEmpty();
    }

    private static boolean isFieldSetAndTrue(ObjectNode value, String fieldName) {
        JsonNode jsonNode = value.get(fieldName);
        return jsonNode != null && jsonNode.asText().equals("true");
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
                    .sluttdato(ressursAggregate.hasNonNull("sluttdato") ? LocalDate.parse(ressursAggregate.get("sluttdato").asText()) : null)
                    .sektor(ressursAggregate.hasNonNull("sektor") ? Sektor.valueOf(ressursAggregate.get("sektor").asText()) : null)
                    .orgtilknytning(orgtilknytning)
                    .build();
        } catch (Exception e) {
            log.error("Error mapping '{}' and '{}' to RessursOpprettetEvent", ressursAggregate, orgtilknytningAggregate, e);
            throw e;
        }
    }

}
