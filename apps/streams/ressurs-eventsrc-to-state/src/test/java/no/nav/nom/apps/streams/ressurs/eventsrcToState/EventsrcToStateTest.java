package no.nav.nom.apps.streams.ressurs.eventsrcToState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import no.nav.nom.apps.streams.ressurs.eventsrcToState.EventsrcToState;
import no.nav.nom.apps.streams.ressurs.eventsrcToState.KafkaConfig;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.*;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
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
    private TestInputTopic<String, RessursEvent> inputTopic;
    private TestOutputTopic<String, RessursState> outputTopic;

    @BeforeEach
    public void setup(){
        setupKafkaTransformer();
    }

    @Test
    @DisplayName("RessursOpprettetEventV1 oppretter RessursState i output topic")
    public void test11(){
        final RessursOpprettetEventV1.RessursOpprettetEventData eventData = new RessursOpprettetEventV1.RessursOpprettetEventData(
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
                null,
                null
        );

        final RessursEvent event = new RessursOpprettetEventV1(
                UUID.randomUUID().toString(),
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
                        KeyValue.pair(event.getFid(), expected)
                );
    }


    @Test
    @DisplayName("Merger RessursSluttet event merges sammen inn i state")
    public void test21(){
        final RessursOpprettetEventV1.RessursOpprettetEventData eventData = new RessursOpprettetEventV1.RessursOpprettetEventData(
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
                null,
                null
        );

        final RessursEvent event = new RessursOpprettetEventV1(
                UUID.randomUUID().toString(),
                eventData,
                (Event.Metadata) null,
                additionalData

        );

        final RessursSluttetEventV1.RessursSluttetEventData sluttetEventData = new RessursSluttetEventV1.RessursSluttetEventData(LocalDate.now());
        final RessursEvent sluttetEvent = new RessursSluttetEventV1(
                event.getFid(),
                sluttetEventData,
                (Event.Metadata) null
        );


        final RessursState expectedOpprettet = new RessursState(
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

        final RessursState expectedSluttet = new RessursState(
                event.getFid(),
                eventData.personident(),
                additionalData.navident().get(),
                eventData.sektor(),
                new Visningsnavn(
                        "fornavn",
                        Optional.empty(),
                        "etternavn"
                ),
                Optional.of(sluttetEventData.sluttdato())
        );


        inputTopic.pipeInput(UUID.randomUUID().toString(), event);
        inputTopic.pipeInput(UUID.randomUUID().toString(), sluttetEvent);


        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(event.getFid(), expectedOpprettet),
                        KeyValue.pair(event.getFid(), expectedSluttet)
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

        inputTopic = topologyTestDriver.createInputTopic("inputTopic", new StringSerializer(), objectSerde.copyWithType(RessursEvent.class).serializer());

        final var ressursStateSerde = objectSerde.copyWithType(RessursState.class);
        ressursStateSerde.deserializer().addTrustedPackages(RessursState.class.getPackageName());
        outputTopic = topologyTestDriver.createOutputTopic("outputTopic", new StringDeserializer(), ressursStateSerde.deserializer());
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
