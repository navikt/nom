package no.nav.nom.apps.streams.kafkaapi.hendelser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import no.nav.nom.apps.streams.kafkaapi.hendelser.v1.Hendelse;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
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

public class EventsrcToHendelseTestTopology {
    ObjectMapper objectMapper = configureObjectMapper();

    TestInputTopic<String, OrgEnhetEvent> orgEnhetInputTopic;
    TestInputTopic<String, RessursEvent> resursInputTopic;
    TestInputTopic<String, OrganiseringEvent> organiseringInputTopic;
    TestInputTopic<String, OrgtilknytningEvent> orgtilknytningInputTopic;

    TestOutputTopic<String, Hendelse> outputTopic;

    @BeforeEach
    public void setup() {
        setupKafkaTransformer();
    }

    private void setupKafkaTransformer() {
        KafkaConfig kafkaConfig = new KafkaConfig();
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final var objectSerde = kafkaConfig.objectSerde(objectMapper);

        new EventsrcToHendelser(
            "orgEnhetInputTopic",
            "resursInputTopic",
            "organiseringInputTopic",
            "orgtilknytningInputTopic",
            "outputTopic",
            objectSerde,
            streamsBuilder
        );

        Topology topology = streamsBuilder.build();

        TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology, new Properties());

        orgEnhetInputTopic = topologyTestDriver
            .createInputTopic("orgEnhetInputTopic", new StringSerializer(), objectSerde.copyWithType(OrgEnhetEvent.class).serializer());
        resursInputTopic = topologyTestDriver
            .createInputTopic("resursInputTopic", new StringSerializer(), objectSerde.copyWithType(RessursEvent.class).serializer());
        organiseringInputTopic = topologyTestDriver
            .createInputTopic("organiseringInputTopic", new StringSerializer(), objectSerde.copyWithType(OrganiseringEvent.class).serializer());
        orgtilknytningInputTopic = topologyTestDriver
            .createInputTopic("orgtilknytningInputTopic", new StringSerializer(), objectSerde.copyWithType(OrgtilknytningEvent.class).serializer());

        final var hendelserStateSerde = objectSerde.copyWithType(Hendelse.class);
        hendelserStateSerde.deserializer().addTrustedPackages(Hendelse.class.getPackageName());

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
