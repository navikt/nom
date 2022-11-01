package no.nav.nom.apps.kafka.aggregate.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

@Component
class KafkaTransformer {

    public KafkaTransformer(
            @Value("${app.kafka.eventsourceTopic}") String eventsourceTopic,
            @Value("${app.kafka.stateTopic}") String stateTopic,
            ObjectMapper objectMapper,
            JsonSerde<Object> objectSerde,
            StreamsBuilder streamsBuilder
    ){
        final var eventSerde = objectSerde.copyWithType(Event.class);
        final var eventDataSerde = objectSerde.copyWithType(EventStateAggregate.class);
        final var ressursStateSerde = objectSerde.copyWithType(RessursState.class);
        final var stringSerde = Serdes.String();

        final var objectNodeSerde = objectSerde.copyWithType(ObjectNode.class);



        streamsBuilder.stream(eventsourceTopic, Consumed.with(stringSerde, eventSerde))
                .groupBy((k, v) -> v.getFid().toString(), Grouped.with(stringSerde, eventSerde))
                .aggregate(
                        () -> new EventStateAggregate(new LinkedHashMap<>(), new LinkedList<>()),
                        (key, event, state) -> {
                            state.data().putAll(event.getData().toMap());
                            state.events().add(event);
                            return state;
                        },
                        Materialized.with(stringSerde, eventDataSerde)
                )
                .mapValues((readOnlyKey, value) -> toRessursState(UUID.fromString(readOnlyKey), value))
                .toStream().to(stateTopic, Produced.with(stringSerde, ressursStateSerde));
    }

    public RessursState toRessursState(UUID fid, EventStateAggregate eventStateAggregate){
        Map<String, String> visningsnavn = (Map<String, String>)eventStateAggregate.data().get("visningsnavn");

        return new RessursState(
                fid,
                eventStateAggregate.getAsOptional("personident").map(Object::toString).orElseThrow(),
                eventStateAggregate.getAsOptional("navident").map(Object::toString).orElseThrow(),
                eventStateAggregate.getAsOptional("sektor").map(Object::toString).map(RessursState.Sektor::valueOf).orElseThrow(),
                new RessursState.Visningsnavn(
                        visningsnavn.get("fornavn"),
                        visningsnavn.get("mellomnavn"),
                        visningsnavn.get("etternavn")
                )
        );
    }
}
