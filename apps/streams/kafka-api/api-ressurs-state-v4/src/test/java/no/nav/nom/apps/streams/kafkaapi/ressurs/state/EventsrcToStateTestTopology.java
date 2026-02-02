package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v4.RessursState;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class EventsrcToStateTestTopology {
    JsonMapper jsonMapper = configureJsonMapper();
    TestInputTopic<String, RessursEvent> ressursInputTopic;
    TestInputTopic<String, OrgtilknytningEvent> orgtilknytningInputTopic;
    TestInputTopic<String, String> navidentInputTopic;
    TestOutputTopic<String, RessursState> outputTopic;

    final AppProperties appProperties = mock(AppProperties.class);

    static final String RESSURS_INPUT_TOPIC = "ressursInputTopic";
    static final String ORGTILKNYTNING_INPUT_TOPIC = "orgtilknytningInputTopic";
    static final String NAVIDENT_INPUT_TOPIC = "navidentInputTopic";
    static final String STATE_TOPIC = "stateTopic";

    @BeforeEach
    public void setup() {
        setupKafkaTransformer();
    }

    private void setupKafkaTransformer() {
        KafkaConfig kafkaConfig = new KafkaConfig();
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final var objectSerde = kafkaConfig.objectSerde(jsonMapper);

        when(appProperties.ressursEvtsrcTopic()).thenReturn(RESSURS_INPUT_TOPIC);
        when(appProperties.orgtilknytningEvtsrcTopic()).thenReturn(ORGTILKNYTNING_INPUT_TOPIC);
        when(appProperties.navidentTopic()).thenReturn(NAVIDENT_INPUT_TOPIC);
        when(appProperties.stateTopic()).thenReturn(STATE_TOPIC);

        new EventsrcToState(
            jsonMapper,
            objectSerde,
            streamsBuilder,
            appProperties
        );

        Topology topology = streamsBuilder.build();

        TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology, new Properties());

        ressursInputTopic = topologyTestDriver.createInputTopic(RESSURS_INPUT_TOPIC, new StringSerializer(), objectSerde.copyWithType(RessursEvent.class).serializer());
        orgtilknytningInputTopic = topologyTestDriver.createInputTopic(ORGTILKNYTNING_INPUT_TOPIC, new StringSerializer(), objectSerde.copyWithType(
            OrgtilknytningEvent.class).serializer());
        navidentInputTopic = topologyTestDriver.createInputTopic(NAVIDENT_INPUT_TOPIC, new StringSerializer(), new StringSerializer());

        final var ressursStateSerde = objectSerde.copyWithType(RessursState.class);
        ressursStateSerde.deserializer().addTrustedPackages(RessursState.class.getPackageName());
        outputTopic = topologyTestDriver.createOutputTopic(STATE_TOPIC, new StringDeserializer(), ressursStateSerde.deserializer());

        log.debug(topology.describe().toString());
    }

    private static JsonMapper configureJsonMapper() {

        return new JsonMapper();
    }
}

