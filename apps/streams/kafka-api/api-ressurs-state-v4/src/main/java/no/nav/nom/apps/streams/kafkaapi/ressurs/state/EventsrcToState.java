
package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v4.Orgtilknytning;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v4.OrgtilknytningExt;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v4.RessursState;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v4.RessursStateExt;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.*;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorSupplier;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
class EventsrcToState {
    private final static Logger log = LoggerFactory.getLogger(EventsrcToState.class);
    final AppProperties appProperties;

    public EventsrcToState(
        ObjectMapper objectMapper,
        JsonSerde<Object> objectSerde,
        StreamsBuilder streamsBuilder,
        AppProperties appProperties
    ) {
        this.appProperties = appProperties;
        final var stringSerde = Serdes.String();
        final var jsonSerde = objectSerde.copyWithType(ObjectNode.class);

        // input meldinger
        final var ressursEventSerde = objectSerde.copyWithType(RessursEvent.class);
        final var orgtilknytningEventSerde = objectSerde.copyWithType(OrgtilknytningEvent.class);
        // extended input meldinger med record offset og timestamp
        final var ressursEventExtSerde = objectSerde.copyWithType(RessursEventExt.class);
        final var orgtilknytningEventExtSerde = objectSerde.copyWithType(OrgtilknytningEventExt.class);

        final JavaType ressursMapEntryType = objectMapper.getTypeFactory().constructParametricType(Map.Entry.class, String.class, RessursStateExt.class);
        final JavaType orgtilknytningMapType = objectMapper.getTypeFactory().constructParametricType(Map.class, String.class, OrgtilknytningExt.class);

        // Aggreger ressurs evtsrc til en Json med sum av verdier, returner per navident
        var ressursAggregateKTable = streamsBuilder.stream(appProperties.ressursEvtsrcTopic(), Consumed.with(stringSerde, ressursEventSerde))
            .filter((k, v) -> !v.getFid().startsWith("FID_MANUELL")) // Ikke behandle manuelle test ressurser/orgtilknytninger
            .processValues(new TransformToRessursEventExtendedMedRecordMetadata()) // Legg på offset/timestamp metadata
            .groupBy((k, v) -> v.ressursEvent().getFid(), Grouped.with(stringSerde, ressursEventExtSerde))
            .aggregate(
                objectMapper::createObjectNode,
                (key, extEvent, state) -> {
                    var event = extEvent.ressursEvent();
                    state.put("kafkaOffset", extEvent.messageMetadata().offset());
                    state.put("kafkaTimestamp", extEvent.messageMetadata().timestamp());

                    if (event instanceof RessursTekniskSlettetEventV1) {
                        state.put("RESSURS TOMBSTONE", true);
                    } else {
                        if (event.getAdditionalData() instanceof RessursOpprettetEventV1.RessursOpprettetAdditionalData data) {
                            if (data.navident() != null) {
                                state.put("navident", data.navident());
                            }
                            if (data.startdato() != null) {
                                state.put("startdato", String.valueOf(data.startdato()));
                            }
                        }
                        if (event.getData() == null && event instanceof RessursJobbtelefonOppdatertEventV1 && event.getFid().equals(state.get("fid").asText())) {
                            RessursJobbtelefonOppdatertEventV1 jobbtelefonOppdatertEventV1 = new RessursJobbtelefonOppdatertEventV1(
                                    event.getFid(),
                                    new RessursJobbtelefonOppdatertEventV1.JobbtelefonOppdatertEventData(
                                            state.get("navident").asText(),
                                            null
                                    ),
                                    event.getMetadata().inputMetadata()
                            );
                            state.setAll(objectMapper.convertValue(jobbtelefonOppdatertEventV1.getData(), ObjectNode.class));
                        } else {
                            state.setAll(objectMapper.convertValue(event.getData(), ObjectNode.class));
                        }
                    }
                    state.put("fid", event.getFid()); // Keep fid - Ønsker å benytte denne som key på output
                    return state;
                },
                Materialized.with(stringSerde, jsonSerde)
            );

        // Snu denne til å være per navident
        var ressursPerNavident = ressursAggregateKTable.toStream()
            .selectKey((fid, ressursJson) -> ressursJson.get("navident").asText())
            .groupByKey(Grouped.with(stringSerde, jsonSerde))
            .reduce((oldRessurs, newRessurs) -> newRessurs,  // Keep only newest ressurs - Actually - there will always be one-to-one fid<->navident, so only one will ever emerge
                Materialized.with(stringSerde, jsonSerde)
            );

        // Aggreger orgtilknytning evtsrc til en Json som er sum per tilknytning, returner per fid
        var orgtilknytningAggregateKTable = streamsBuilder.stream(appProperties.orgtilknytningEvtsrcTopic(), Consumed.with(stringSerde, orgtilknytningEventSerde))
            .filter((k, v) -> !v.getFid().startsWith("FID_MANUELL")) // Ikke behandle manuelle test ressurser/orgtilknytninger
            .processValues(new TransformToOrgtilknytningEventExtendedMedRecordMetadata()) // Legg på offset/timestamp metadata
            .groupBy((k, v) -> v.orgtilknytningEvent().getFid(), Grouped.with(stringSerde, orgtilknytningEventExtSerde))
            .aggregate(
                objectMapper::createObjectNode,
                (key, extEvent, state) -> {
                    var event = extEvent.orgtilknytningEvent();
                    state.put("kafkaOffset", extEvent.messageMetadata().offset());
                    state.put("kafkaTimestamp", extEvent.messageMetadata().timestamp());

                    if (event instanceof OrgtilknytningFjernetEventV1) {
                        state.put("OT TOMBSTONE", true);
                        // return null; Create TOMBSTONE - should skip mapValues and get forwarded directly to output stream. -
                        // (See https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Stream+Usage+Patterns#KafkaStreamUsagePatterns-Howtomanagethesizeofstatestoresusingtombstones?)
                        // NOPE ! Does not work. If multiple input event at once, it works, but null event alone will be 'reduced' away
                    } else {
                        state.setAll(objectMapper.convertValue(event.getData(), ObjectNode.class));
                    }
                    state.put("fid", event.getFid());
                    return state;
                },
                Materialized.with(stringSerde, jsonSerde)
            );

        // Map orgtilknytningene i en Map per navident
        KTable<String, Map<String, OrgtilknytningExt>> orgtilknytningMappedToNavident = orgtilknytningAggregateKTable.toStream()
            .selectKey((key, otJson) -> otJson.get("navident").asText())
            .groupByKey(Grouped.with(stringSerde, jsonSerde))
            .aggregate(
                HashMap::new,
                (navident, otJson, map) -> {
                    var fid = otJson.get("fid").asText();
                    if (otJson.has("OT TOMBSTONE")) {
                        map.remove(fid);
                    } else {
                        map.put(otJson.get("fid").asText(), toOrgtilknytningExt(otJson));
                    }
                    return map;
                },
                Materialized.with(stringSerde, objectSerde.copyWithType(orgtilknytningMapType))
            );


        var identType = streamsBuilder.stream(appProperties.navidentTopic(), Consumed.with(stringSerde, stringSerde))
            .mapValues(v -> navidentTopicValueToJson(v, objectMapper))
            .groupBy(
                (k, v) -> v.get("payload").get("navident").asText(),
                Grouped.with(stringSerde, jsonSerde)
            )
            .aggregate(
                () -> "",
                (key, event, state) -> event.get("payload").get("identType").asText(),
                Materialized.with(stringSerde, stringSerde)
            );

        // Left join IdentType
        var withIdentTypeByNavident = ressursPerNavident.leftJoin(
            identType, (a, b) -> {
                if (b != null) {
                    a.put("identType", b);
                }
                return a;
            }
        );

        // Left Join ressurs med orgtilknytninger, re-map til ressurs.fid og send to output
        withIdentTypeByNavident.leftJoin(
                orgtilknytningMappedToNavident,
                this::toRessursStateExt,
                Materialized.with(stringSerde, objectSerde.copyWithType(ressursMapEntryType))
            )
            .toStream()
            .selectKey((navident, ressursEntry) -> ressursEntry.getKey())
            .mapValues(Map.Entry::getValue)
            .filter((k,v) -> !skipRelayMessages(k,v))
            .map(this::toRessursState)
            .filter((s, ressursState) -> ressursState == null ||  (ressursState.sektor() != null && ressursState.identType() != null)) // Påkrevd å ha Sektor og identType
            .to(appProperties.stateTopic(), Produced.with(stringSerde, objectSerde.copyWithType(RessursState.class)));
    }

    private static ObjectNode navidentTopicValueToJson(String value, ObjectMapper objectMapper) {
        try {
            var parsedObjNode = objectMapper.readValue(value, ObjectNode.class);
            var payload = parsedObjNode.get("payload");
            var hasExpectedFields = payload.has("identType") && payload.has("navident");
            if (!hasExpectedFields) {
                throw new RuntimeException("JSON fra navident topic hadde ikke-forventede felter");
            }
            return parsedObjNode;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Feil ved JSON parsing fra navident topic");
        }
    }

    private boolean skipRelayMessages(String s, RessursStateExt ressursStateExt) {
        // Her kan vi skippe meldinger til output hvis vi må resette streamen pga en poison pill eller lignende.
        // Vi har da mulighet til å styre ift offset/timestamp på enten/eller/både/og for RessursEvent og OrgtilknytningerEvent

        var skipUntilOrgtilknytningOffset = appProperties.skipProducingStateEventsUntilOrgtilknytningEvtsrcOffset();
        var skipUntilRessursOffset = appProperties.skipProducingStateEventsUntilRessursEvtsrcOffset();
        var actualOrgtilknytningOffset = ressursStateExt.orgtilknytningMessageMetadata().offset();
        var actualRessursOffset = ressursStateExt.ressursMessageMetadata().offset();

        // Vi hopper over til en av offsetene er nådd
        var produceMessage = actualRessursOffset >=skipUntilRessursOffset || actualOrgtilknytningOffset >= skipUntilOrgtilknytningOffset;
        log.debug("Skip message: {} - RessursOffset: {} < SkipUntilRessursOffset: {} && OrgtilknytningOffset: {} < SkipUntilOrgtilknytningOffset: {}",
            !produceMessage, actualRessursOffset, skipUntilRessursOffset, actualOrgtilknytningOffset, skipUntilOrgtilknytningOffset);

        return !produceMessage;
    }

    public Map.Entry<String, RessursStateExt> toRessursStateExt(JsonNode eventStateAggregate, Map<String, OrgtilknytningExt> orgtilknytningerMap) {
        try {
            final var visningsnavn = eventStateAggregate.get("visningsnavn");
            final var orgtilknytninger = orgtilknytningerMap == null ? null : orgtilknytningerMap.values().stream().map(OrgtilknytningExt::orgtilknytning).toList();

            var orgtilknytningerMetaData = orgtilknytningerMap == null ?
                new MessageMetadata(-1, -1) :
                new MessageMetadata(
                    orgtilknytningerMap.values().stream().mapToLong(ot -> ot.messageMetadata().offset()).max().orElse(0),
                    orgtilknytningerMap.values().stream().mapToLong(ot -> ot.messageMetadata().timestamp()).max().orElse(0)
                );

            var ressursMetaData = new MessageMetadata(
                eventStateAggregate.get("kafkaOffset").asLong(),
                eventStateAggregate.get("kafkaTimestamp").asLong()
            );

            var fid = eventStateAggregate.get("fid").asText(); // Plukk ut igjen Fid til key

            if (eventStateAggregate.hasNonNull("RESSURS TOMBSTONE")) {
                log.info("SlettRessursEvent for fid {} received - create TOMBSTONE in StateTopic", fid);
                return new AbstractMap.SimpleEntry<>(
                        fid, new RessursStateExt(null, ressursMetaData, orgtilknytningerMetaData)
                );
            } else {
                var ressursState = new RessursState(
                        eventStateAggregate.get("personident").asText(),
                        eventStateAggregate.get("navident").asText(),
                        new Visningsnavn(
                                visningsnavn.get("fornavn").asText(),
                                Optional.ofNullable(visningsnavn.hasNonNull("mellomnavn") ? visningsnavn.get("mellomnavn").asText() : null),
                                visningsnavn.get("etternavn").asText()
                        ),
                        eventStateAggregate.hasNonNull("startdato") ? LocalDate.parse(eventStateAggregate.get("startdato").asText()) : null,
                        eventStateAggregate.hasNonNull("sluttdato") ? LocalDate.parse(eventStateAggregate.get("sluttdato").asText()) : null,
                        eventStateAggregate.hasNonNull("sektor") ? Sektor.valueOf(eventStateAggregate.get("sektor").asText()) : null,
                        eventStateAggregate.hasNonNull("identType") ? IdentType.valueOf(eventStateAggregate.get("identType").asText()) : null,
                        getPrimaryTelefonnummer(eventStateAggregate),
                        eventStateAggregate.hasNonNull("primaryFysiskBrukersted") ? eventStateAggregate.get("primaryFysiskBrukersted").asText() : null,
                        orgtilknytninger
                );

                return new AbstractMap.SimpleEntry<>(
                        fid, new RessursStateExt(ressursState, ressursMetaData, orgtilknytningerMetaData)
                );
            }
        } catch (Exception e) {
            log.error("Error mapping to RessursState", e);
            throw e;
        }
    }

    private String getPrimaryTelefonnummer(JsonNode eventStateAggregate) {
        if (eventStateAggregate.hasNonNull("jobbtelefon")) {
            return eventStateAggregate.get("jobbtelefon").get("nummer").asText();
        } else if (eventStateAggregate.hasNonNull("primaryTelefonnummer")) {
            return eventStateAggregate.get("primaryTelefonnummer").asText();
        }
        return null;
    }

    static class TransformToRessursEventExtendedMedRecordMetadata implements FixedKeyProcessorSupplier<String, RessursEvent, RessursEventExt> {
        @Override
        public FixedKeyProcessor<String, RessursEvent, RessursEventExt> get() {
            return new FixedKeyProcessor<>() {
                private FixedKeyProcessorContext<String, RessursEventExt> context;

                @Override
                public void init(FixedKeyProcessorContext<String, RessursEventExt> context) {
                    // keep the processor context locally because we need it in process()
                    this.context = context;
                }

                @Override
                public void process(FixedKeyRecord<String, RessursEvent> inputRecord) {
                    var metaData = new MessageMetadata(context.recordMetadata().get().offset(), inputRecord.timestamp());
                    var extendedEvent = new RessursEventExt(inputRecord.value(), metaData);
                    var outputRecord = inputRecord.withValue(extendedEvent);
                    context.forward(outputRecord);
                }
            };
        }
    }

    public OrgtilknytningExt toOrgtilknytningExt(JsonNode otJson) {
        try {
            var metaData = new MessageMetadata(
                otJson.get("kafkaOffset").asLong(),
                otJson.get("kafkaTimestamp").asLong()
            );

            var ot = new Orgtilknytning(
                otJson.get("fid").asText(),
                otJson.get("orgEnhetId").asText(),
                LocalDate.parse(otJson.get("gyldigFom").asText()),
                otJson.hasNonNull("gyldigTom") ? LocalDate.parse(otJson.get("gyldigTom").asText()) : null,
                otJson.hasNonNull("dagligOppfolging") && otJson.get("dagligOppfolging").asBoolean(),
                otJson.hasNonNull("sektor") ? Sektor.valueOf(otJson.get("sektor").asText()) : null,
                otJson.hasNonNull("orgtilknytningType") ? Tilknytningtype.valueOf(otJson.get("orgtilknytningType").asText()) : null
            );

            return new OrgtilknytningExt(ot, metaData);
        } catch (Exception e) {
            log.error("Error mapping to Orgtilknytning", e);
            throw e;
        }
    }

    static class TransformToOrgtilknytningEventExtendedMedRecordMetadata implements FixedKeyProcessorSupplier<String, OrgtilknytningEvent, OrgtilknytningEventExt> {
        @Override
        public FixedKeyProcessor<String, OrgtilknytningEvent, OrgtilknytningEventExt> get() {
            return new FixedKeyProcessor<>() {
                private FixedKeyProcessorContext<String, OrgtilknytningEventExt> context;

                @Override
                public void init(FixedKeyProcessorContext<String, OrgtilknytningEventExt> context) {
                    // keep the processor context locally because we need it in process()
                    this.context = context;
                }

                @Override
                public void process(FixedKeyRecord<String, OrgtilknytningEvent> inputRecord) {
                    var metaData = new MessageMetadata(context.recordMetadata().get().offset(), inputRecord.timestamp());
                    var extendedEvent = new OrgtilknytningEventExt(inputRecord.value(), metaData);
                    var outputRecord = inputRecord.withValue(extendedEvent);
                    context.forward(outputRecord);
                }
            };
        }
    }

    private KeyValue<String, RessursState> toRessursState(String s, RessursStateExt ressursStateExt) {
        return new KeyValue<>(s, ressursStateExt.ressursState());
    }
}
