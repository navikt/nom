package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser.v1.RessursOpprettetEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import java.util.Properties;

import static org.mockito.Mockito.when;

@Slf4j
public class EventsourceToExternalEventsTestTopology {

    static final String RESSURS_INPUT_TOPIC = "ressursInputTopic";
    static final String ORGTILKNYTNING_INPUT_TOPIC = "orgtilknytningInputTopic";
    static final String RESSURS_HENDELSE_OUTPUT_TOPIC = "ressursHendelseOutputTopic";

    ObjectMapper objectMapper = configureObjectMapper();

    private TopologyTestDriver topologyTestDriver;

    TestInputTopic<String, RessursEvent> ressursInputTopic;
    TestInputTopic<String, OrgtilknytningEvent> orgtilknytningInputTopic;
    TestOutputTopic<String, RessursOpprettetEvent> outputTopic;

    @Mock
    private AppProperties appProperties;


    @BeforeEach
    public void setup() {
        setupKafkaTransformer();
    }

    @AfterEach
    public void tearDown() {
        if (topologyTestDriver != null) {
            topologyTestDriver.close();
        }
    }

    private void setupKafkaTransformer() {
        KafkaConfig kafkaConfig = new KafkaConfig();
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final var objectSerde = kafkaConfig.objectSerde(objectMapper);

        when(appProperties.ressursEvtsrcTopic()).thenReturn(RESSURS_INPUT_TOPIC);
        when(appProperties.orgtilknytningEvtsrcTopic()).thenReturn(ORGTILKNYTNING_INPUT_TOPIC);
        when(appProperties.ressursHendelseTopic()).thenReturn(RESSURS_HENDELSE_OUTPUT_TOPIC);


        new EventsourceToExternalEvents(
                objectMapper,
                objectSerde,
                streamsBuilder,
                appProperties
        );


        Topology topology = streamsBuilder.build();
        topologyTestDriver = new TopologyTestDriver(topology, new Properties());

//        log.info(topology.describe().toString());

        ressursInputTopic = topologyTestDriver.createInputTopic(RESSURS_INPUT_TOPIC, new StringSerializer(), objectSerde.copyWithType(RessursEvent.class).serializer());
        orgtilknytningInputTopic = topologyTestDriver.createInputTopic(ORGTILKNYTNING_INPUT_TOPIC, new StringSerializer(), objectSerde.copyWithType(OrgtilknytningEvent.class).serializer());

        final var ressursOpprettetSerde = objectSerde.copyWithType(RessursOpprettetEvent.class);
        ressursOpprettetSerde.deserializer().addTrustedPackages(RessursOpprettetEvent.class.getPackageName());
        outputTopic = topologyTestDriver.createOutputTopic(RESSURS_HENDELSE_OUTPUT_TOPIC, new StringDeserializer(), ressursOpprettetSerde.deserializer());

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
