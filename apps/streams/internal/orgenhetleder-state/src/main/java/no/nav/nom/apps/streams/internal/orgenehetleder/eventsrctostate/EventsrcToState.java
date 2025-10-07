package no.nav.nom.apps.streams.internal.orgenehetleder.eventsrctostate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederEvent;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederSlettetEventV1;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
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

import static java.util.Objects.nonNull;

@Component
public class EventsrcToState {
    private final static Logger log = LoggerFactory.getLogger(EventsrcToState.class);

    public EventsrcToState(
        @Value("${app.kafka.eventsourceTopic}") String eventsourceTopic,
        @Value("${app.kafka.stateTopic}") String stateTopic,
        ObjectMapper objectMapper,
        JsonSerde<Object> objectSerde,
        StreamsBuilder streamsBuilder
    ) {
        final var stringSerde = Serdes.String();
        final var eventSerde = objectSerde.copyWithType(OrgEnhetLederEvent.class);

        streamsBuilder.stream(eventsourceTopic, Consumed.with(stringSerde, eventSerde))
            .groupBy((key, value) -> value.getFid(), Grouped.with(stringSerde, eventSerde))
            .aggregate(
                objectMapper::createObjectNode,
                (key, event, state) -> {
                    if (event instanceof OrgEnhetLederSlettetEventV1) {
                        state.put("TOMBSTONE", true);
                        // return null; // Create TOMBSTONE - should skip mapValues and get forwarded directly to output stream. -
                        // (See https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Stream+Usage+Patterns#KafkaStreamUsagePatterns-Howtomanagethesizeofstatestoresusingtombstones?)
                        // NOPE ! Does not work. If multiple input event at once, it works, but null event alone will be 'reduced' away
                    }
                    state.setAll(objectMapper.convertValue(event.getData(), ObjectNode.class));
                    if (nonNull(event.getMetadata()) && nonNull(event.getMetadata().inputMetadata().origination())) {
                        var origination = event.getMetadata().inputMetadata().origination();
                        if (nonNull(origination.initiatedBy())) state.put("sistEndretAv",  origination.initiatedBy());
                    }
                    return state;
                },
                Materialized.with(stringSerde, objectSerde.copyWithType(ObjectNode.class))
            )
            .mapValues(this::toOrgEnhetLederState)
            .toStream()
            .to(stateTopic, Produced.with(stringSerde, objectSerde.copyWithType(OrgEnhetLederState.class)));
    }

    public OrgEnhetLederState toOrgEnhetLederState(String fid, JsonNode eventStateAggregate) {
        try {
            if (eventStateAggregate.hasNonNull("TOMBSTONE")) {
                return null;
            }
            return new  OrgEnhetLederState(
                fid,
                eventStateAggregate.get("navident").asText(),
                eventStateAggregate.get("orgEnhetId").asText(),
                eventStateAggregate.hasNonNull("startdato") ? LocalDate.parse(eventStateAggregate.get("startdato").asText()) : null,
                eventStateAggregate.hasNonNull("sluttdato") ? LocalDate.parse(eventStateAggregate.get("sluttdato").asText()) : null,
                eventStateAggregate.get("sistEndretAv").asText()
            );
        } catch (Exception ex) {
            log.error("Error mapping to OrgEnhetLederState", ex);
            throw ex;
        }

    }
}
