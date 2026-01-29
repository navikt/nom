package no.nav.nom.apps.streams.kafkaapi.hendelser;

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
import tools.jackson.databind.json.JsonMapper;

import java.util.Properties;

public class EventsrcToHendelseTestTopology {
    JsonMapper jsonMapper = configureJsonMapper();

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
        final var objectSerde = kafkaConfig.objectSerde(jsonMapper);

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

    private static JsonMapper configureJsonMapper() {

        return new JsonMapper();
    }
}
