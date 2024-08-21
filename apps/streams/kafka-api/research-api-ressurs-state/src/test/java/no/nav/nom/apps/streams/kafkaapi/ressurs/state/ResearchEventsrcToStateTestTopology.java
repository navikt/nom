package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v2.RessursState;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.BeforeEach;

import java.util.Properties;

public class ResearchEventsrcToStateTestTopology {
    ObjectMapper objectMapper = configureObjectMapper();
    TestInputTopic<String, RessursEvent> ressursInputTopic;
    TestInputTopic<String, OrgtilknytningEvent> orgtilknytningInputTopic;
    TestOutputTopic<String, RessursState> outputTopic;

    @BeforeEach
    public void setup() {
        setupKafkaTransformer();
    }

    private void setupKafkaTransformer() {
        KafkaConfig kafkaConfig = new KafkaConfig();
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final var objectSerde = kafkaConfig.objectSerde(objectMapper);


        new ResearchEventsrcToState(
            "ressursInputTopic",
            "orgtilknytningInputTopic",
            "outputTopic",
            objectMapper,
            objectSerde,
            streamsBuilder
        );

        Topology topology = streamsBuilder.build();

        TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology, new Properties());

        //        log.debug(topology.describe().toString());

        ressursInputTopic = topologyTestDriver.createInputTopic("ressursInputTopic", new StringSerializer(), objectSerde.copyWithType(RessursEvent.class).serializer());
        orgtilknytningInputTopic = topologyTestDriver.createInputTopic("orgtilknytningInputTopic", new StringSerializer(), objectSerde.copyWithType(
            OrgtilknytningEvent.class).serializer());

        final var ressursStateSerde = objectSerde.copyWithType(RessursState.class);
        ressursStateSerde.deserializer().addTrustedPackages(RessursState.class.getPackageName());
        outputTopic = topologyTestDriver.createOutputTopic("outputTopic", new StringDeserializer(), ressursStateSerde.deserializer());
    }

    private static ObjectMapper configureObjectMapper() {
        final var objectMapper = new ObjectMapper();

        return objectMapper.findAndRegisterModules()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
}
