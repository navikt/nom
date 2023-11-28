package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v1.HendelserState;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.junit.jupiter.api.BeforeEach;

import java.util.Properties;

public class EventsrcToStateTestTopology {
    ObjectMapper objectMapper = configureObjectMapper();
    TestInputTopic<String, OrgEnhetEvent> orgEnhetInputTopic;
    TestInputTopic<String, RessursEvent> resursInputTopic;

    TestOutputTopic<String, HendelserState> outputTopic;

    @BeforeEach
    public void setup() {
        setupKafkaTransformer();
    }

    private void setupKafkaTransformer() {
        KafkaConfig kafkaConfig = new KafkaConfig();
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final var objectSerde = kafkaConfig.objectSerde(objectMapper);


        new EventsrcToState(
            "orgEnhetInputTopic",
            "resursInputTopic",
            "outputTopic",
            objectMapper,
            objectSerde,
            streamsBuilder
        );

        Topology topology = streamsBuilder.build();

        TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology, new Properties());

        orgEnhetInputTopic = topologyTestDriver
            .createInputTopic("orgEnhetInputTopic", new StringSerializer(), objectSerde.copyWithType(OrgEnhetEvent.class).serializer());
        resursInputTopic = topologyTestDriver
            .createInputTopic("resursInputTopic", new StringSerializer(), objectSerde.copyWithType(RessursEvent.class).serializer());

        final var hendelserStateSerde = objectSerde.copyWithType(HendelserState.class);
        hendelserStateSerde.deserializer().addTrustedPackages(HendelserState.class.getPackageName());

        outputTopic = topologyTestDriver.createOutputTopic("outputTopic", new StringDeserializer(), hendelserStateSerde.deserializer());
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
