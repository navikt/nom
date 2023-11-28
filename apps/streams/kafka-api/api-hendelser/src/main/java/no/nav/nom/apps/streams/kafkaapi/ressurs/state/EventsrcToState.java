package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v1.HendelserState;
import no.nav.nom.contract.nom.EntityType;
import no.nav.nom.contract.nom.HendelserType;
import no.nav.nom.contract.nom.eventsource.OrgEnhetAvvikletEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetKorrigertEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
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

import java.time.LocalDateTime;


@Component
public class EventsrcToState {
    private final static Logger log = LoggerFactory.getLogger(EventsrcToState.class);

    public EventsrcToState(
        @Value("${app.kafka.orgenhet-evtsrc-topic}") String orgenhetEvtsrcTopic,
        @Value("${app.kafka.ressurs-evtsrc-topic}") String ressursEvtsrcTopic,
        @Value("${app.kafka.state-topic.v1}") String stateTopic,
        ObjectMapper objectMapper,
        JsonSerde<Object> objectSerde,
        StreamsBuilder streamsBuilder
    ) {
        final var stringSerde = Serdes.String();
        final var jsonSerde = objectSerde.copyWithType(ObjectNode.class);
        final var orgenhetEventSerde = objectSerde.copyWithType(OrgEnhetEvent.class);
        final var organiseringEventSerde = objectSerde.copyWithType(OrganiseringEvent.class);
        final var ressursEventSerde = objectSerde.copyWithType(RessursEvent.class);
        final var hendelserStateSerde = objectSerde.copyWithType(no.nav.nom.apps.streams.kafkaapi.ressurs.state.v1.HendelserState.class);

        // orgenhet
        streamsBuilder.stream(orgenhetEvtsrcTopic, Consumed.with(stringSerde, orgenhetEventSerde))
            .groupBy((k, v) -> v.getFid(), Grouped.with(stringSerde, orgenhetEventSerde))
            .aggregate(
                objectMapper::createObjectNode,
                (key, event, state) -> {
                    HendelserState hendelserState = convert(event);
                    state.setAll(objectMapper.convertValue(hendelserState, ObjectNode.class));
                    return state;
                },
                Materialized.with(stringSerde, jsonSerde)
            )
            .mapValues(this::toHendelserState)
            .toStream().to(stateTopic, Produced.with(stringSerde, hendelserStateSerde));

        // resurs
        /*
        streamsBuilder.stream(ressursEvtsrcTopic, Consumed.with(stringSerde, organiseringEventSerde))
            .peek((k, v) -> {
                log.info("Resurs Event received: {}", v);
                }
            );
            */


    }

    private HendelserState toHendelserState(String fid, JsonNode eventStateAggregate) {
        try {
            return new no.nav.nom.apps.streams.kafkaapi.ressurs.state.v1.HendelserState(
                eventStateAggregate.get("id").asText(),
                LocalDateTime.parse(eventStateAggregate.get("timestamp").asText()),
                EntityType.valueOf(eventStateAggregate.get("entityType").asText()),
                HendelserType.valueOf(eventStateAggregate.get("hendelserType").asText())
            );
        } catch (Exception e) {
            log.error("Error mapping to OrganiseringState", e);
            throw e;
        }
    }

    private HendelserState convert(OrgEnhetEvent event) {
        try {
            var hendelserStateBuilder = HendelserState.builder()
                .entityType(EntityType.ORGENHET)
                .timestamp(event.getMetadata() != null? event.getMetadata().createdTimestamp(): LocalDateTime.now());

            if (event.getData() instanceof OrgEnhetOpprettetEventV1.OrgEnhetOpprettetEventData data) {
                hendelserStateBuilder
                    .id(data.id())
                    .hendelserType(HendelserType.NY);
            } else if (event.getData() instanceof OrgEnhetKorrigertEventV1.OrgEnhetKorrigertEventData data) {
                hendelserStateBuilder
                    .id(data.id())
                    .hendelserType(HendelserType.ENDRET);
            } else if (event.getData() instanceof OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData) {
                hendelserStateBuilder
                    .hendelserType(HendelserType.ENDRET);
            } else if (event.getData() instanceof OrgEnhetAvvikletEventV1.OrgEnhetAvvikletEventData) {
                hendelserStateBuilder
                    .hendelserType(HendelserType.FJERNET);
            }
            return hendelserStateBuilder.build();
        } catch (Exception e) {
            log.error("Error mapping to HendelserState", e);
            throw e;
        }
    }
}
