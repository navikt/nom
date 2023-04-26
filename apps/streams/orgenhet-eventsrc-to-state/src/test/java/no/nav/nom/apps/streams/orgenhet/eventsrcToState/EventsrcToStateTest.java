package no.nav.nom.apps.streams.orgenhet.eventsrcToState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetOpprettetEventV1;
import no.nav.nom.contract.nom.state.OrgEnhetState;
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

import java.time.LocalDate;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public class EventsrcToStateTest {
    private ObjectMapper objectMapper = configureObjectMapper();
    private TestInputTopic<String, OrgEnhetEvent> inputTopic;
    private TestOutputTopic<String, OrgEnhetState> outputTopic;

    @BeforeEach
    public void setup(){
        setupKafkaTransformer();
    }

    @Test
    @DisplayName("OrgEnhetOpprettetEventV1 oppretter OrgEnhetState i output topic")
    public void test11(){
        final OrgEnhetOpprettetEventV1.OrgEnhetOpprettetEventData eventData = new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetEventData(
                "DummyIntenrnId_1",
                "TestName",
                LocalDate.of(2022, 12, 01),
                Optional.of("orgEnhet_123"),
                Optional.of("ressurs_123")
        );


        final OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalAgressoData additionalData = new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalAgressoData(
                "AgressoId_123",
                "AgressoNavn",
                "AgressoOrgNivaa_1",
                Optional.of("AgressoOrgEnhetType"),
                "AgressoId_123-AgressoNavn-AgressoOrgNivaa_1"
        );

        final OrgEnhetEvent event = new OrgEnhetOpprettetEventV1(
                UUID.randomUUID().toString(),
                eventData,
                (Event.Metadata) null,
                additionalData
        );

        final OrgEnhetState expected = new OrgEnhetState(
                event.getFid(),
                eventData.id(),
                eventData.navn(),
                eventData.gyldigFom(),
                Optional.empty(),
                eventData.overordnetOrgEnhetFid(),
                eventData.lederRessursFid(),
                Optional.empty(),
            Optional.empty(),
                Optional.ofNullable(additionalData.uniqueAgressoId()),
                Optional.ofNullable(additionalData.agressoId()),
                Optional.ofNullable(additionalData.agressoOrgNivaa()),
                additionalData.agressoOrgenhetType()
        );

        inputTopic.pipeInput(UUID.randomUUID().toString(), event);


        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(event.getFid(), expected)
                );
    }

    private void setupKafkaTransformer(){
        KafkaConfig kafkaConfig = new KafkaConfig();
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final var objectSerde = kafkaConfig.objectSerde(objectMapper);


        new EventsrcToState(
                "inputTopic",
                "outputTopic",
                objectMapper,
                objectSerde,
                streamsBuilder
        );

        Topology topology = streamsBuilder.build();

        TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology, new Properties());

        inputTopic = topologyTestDriver.createInputTopic("inputTopic", new StringSerializer(), objectSerde.copyWithType(OrgEnhetEvent.class).serializer());

        final var orgEnhetStateSerde = objectSerde.copyWithType(OrgEnhetState.class);
        orgEnhetStateSerde.deserializer().addTrustedPackages(OrgEnhetState.class.getPackageName());
        outputTopic = topologyTestDriver.createOutputTopic("outputTopic", new StringDeserializer(), orgEnhetStateSerde.deserializer());
    }

    private static ObjectMapper configureObjectMapper(){
        final var objectMapper = new ObjectMapper();

        return objectMapper.findAndRegisterModules()
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
}
