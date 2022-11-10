package no.nav.nom.apps.kafka.aggregate.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public class KafkaTransformerTest {
    private ObjectMapper objectMapper = configureObjectMapper();
    private TestInputTopic<String, Event> inputTopic;
    private TestOutputTopic<String, RessursState> outputTopic;

    @BeforeEach
    public void setup(){
        setupKafkaTransformer();
    }

    @Test
    @DisplayName("RessursOpprettetEventV1 oppretter RessursState i output topic")
    public void test(){
        final RessursOpprettetEventV1.RessursRegisteredEventData eventData = new RessursOpprettetEventV1.RessursRegisteredEventData(
                "personident",
                Sektor.NAV_STATLIG,
                new Visningsnavn(
                        "fornavn",
                        null,
                        "etternavn"
                )
        );

        final RessursOpprettetEventV1.RessursOpprettetAdditionalData additionalData = new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
                Optional.of("navident"),
                null,
                null
        );

        final Event event = new RessursOpprettetEventV1(
                UUID.randomUUID(),
                eventData,
                (Event.Metadata) null,
                additionalData

        );

        final RessursState expected = new RessursState(
                event.getFid(),
                eventData.personident(),
                additionalData.navident().get(),
                eventData.sektor(),
                new Visningsnavn(
                        "fornavn",
                        Optional.empty(),
                        "etternavn"
                ),
                Optional.empty()
        );

        inputTopic.pipeInput(UUID.randomUUID().toString(), event);


        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(event.getFid().toString(), expected)
                );
    }

    private void setupKafkaTransformer(){
        KafkaConfig kafkaConfig = new KafkaConfig();
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final var objectSerde = kafkaConfig.objectSerde(objectMapper);


        new KafkaTransformer(
                "inputTopic",
                "outputTopic",
                objectMapper,
                objectSerde,
                streamsBuilder
        );

        Topology topology = streamsBuilder.build();

        TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology, new Properties());

        inputTopic = topologyTestDriver.createInputTopic("inputTopic", new StringSerializer(), objectSerde.copyWithType(Event.class).serializer());

        final var ressursStateSerde = objectSerde.copyWithType(RessursState.class);
        ressursStateSerde.deserializer().addTrustedPackages(RessursState.class.getPackageName());
        outputTopic = topologyTestDriver.createOutputTopic("outputTopic", new StringDeserializer(), ressursStateSerde.deserializer());
    }

    private static ObjectMapper configureObjectMapper(){
        final var objectMapper = new ObjectMapper();

        return objectMapper.findAndRegisterModules()
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
    }
}
