package no.nav.nom.apps.streams.internal.ressurs.eventsrcToState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursTekniskSlettetEventV1;
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

import java.time.LocalDate;
import java.util.*;

public class EventsrcToStateTest {
    private ObjectMapper objectMapper = configureObjectMapper();
    private TestInputTopic<String, RessursEvent> inputTopic;
    private TestOutputTopic<String, RessursState> outputTopic;

    // Fjerne dette og heller ha tids-uavhengige tester?
    private LocalDate mockToday = LocalDate.of(2023, 1, 1);

    @BeforeEach
    public void setup() {
        setupKafkaTransformer();
    }

    @Test
    @DisplayName("RessursOpprettetEventV1 oppretter RessursState i output topic")
    public void test11() {
        final RessursOpprettetEventV1.RessursOpprettetEventData eventData = new RessursOpprettetEventV1.RessursOpprettetEventData(
                "personident",
                Sektor.NAV_STATLIG,
                new Visningsnavn(
                        "fornavn",
                        null,
                        "etternavn"
                ),
                List.of("2011"),
                "2011"
        );

        final RessursOpprettetEventV1.RessursOpprettetAdditionalData additionalData = new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
                Optional.of("navident"),
                null,
                null,
                null,
                Optional.of(mockToday),
                Optional.empty(),
                Optional.empty()
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
                Optional.empty(),
                Optional.of(mockToday),
                Optional.ofNullable(eventData.primaryFysiskBrukersted()),
                eventData.fysiskeBrukersteder()
        );

        inputTopic.pipeInput(UUID.randomUUID().toString(), event);


        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(event.getFid(), expected)
                );
    }


    @Test
    @DisplayName("Merger RessursSluttet event merges sammen inn i state")
    public void test21() {
        final RessursOpprettetEventV1.RessursOpprettetEventData eventData = new RessursOpprettetEventV1.RessursOpprettetEventData(
                "personident",
                Sektor.NAV_STATLIG,
                new Visningsnavn(
                        "fornavn",
                        null,
                        "etternavn"
                ),
                List.of("2011"),
                "2011"
        );

        final RessursOpprettetEventV1.RessursOpprettetAdditionalData additionalData = new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
                Optional.of("navident"),
                null,
                null,
                null,
                Optional.of(mockToday),
                Optional.empty(),
                Optional.empty()
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
                Optional.empty(),
                Optional.of(mockToday),
                Optional.ofNullable(eventData.primaryFysiskBrukersted()),
                eventData.fysiskeBrukersteder()
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
                Optional.of(sluttetEventData.sluttdato()),
                Optional.of(mockToday),
                Optional.ofNullable(eventData.primaryFysiskBrukersted()),
                eventData.fysiskeBrukersteder()
        );


        inputTopic.pipeInput(UUID.randomUUID().toString(), event);
        inputTopic.pipeInput(UUID.randomUUID().toString(), sluttetEvent);


        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(event.getFid(), expectedOpprettet),
                        KeyValue.pair(event.getFid(), expectedSluttet)
                );
    }

    @Test
    @DisplayName("Merger PersonopplysningerEndret event merges sammen inn i state")
    public void test31() {
        final RessursOpprettetEventV1.RessursOpprettetEventData eventData = new RessursOpprettetEventV1.RessursOpprettetEventData(
                "personident",
                Sektor.NAV_STATLIG,
                new Visningsnavn(
                        "fornavn",
                        null,
                        "etternavn"
                ),
                List.of("2011"),
                "2011"
        );

        final RessursOpprettetEventV1.RessursOpprettetAdditionalData additionalData = new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
                Optional.of("navident"),
                null,
                null,
                null,
                Optional.of(mockToday),
                Optional.empty(),
                Optional.empty()
        );

        final RessursEvent event = new RessursOpprettetEventV1(
                UUID.randomUUID().toString(),
                eventData,
                (Event.Metadata) null,
                additionalData

        );

        final RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData personaliaOppdatertEventData = new RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData(
                new Visningsnavn(
                        "fornavnEndret",
                        Optional.empty(),
                        "etternavnEndret"
                )
        );
        final RessursEvent personopplysningerEndretEvent = new RessursPersonaliaOppdatertV1(
                event.getFid(),
                personaliaOppdatertEventData,
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
                Optional.empty(),
                Optional.of(mockToday),
                Optional.ofNullable(eventData.primaryFysiskBrukersted()),
                eventData.fysiskeBrukersteder()
        );

        final RessursState expectedEndret = new RessursState(
                event.getFid(),
                eventData.personident(),
                additionalData.navident().get(),
                eventData.sektor(),
                personaliaOppdatertEventData.visningsnavn(),
                Optional.empty(),
                Optional.of(mockToday),
                Optional.ofNullable(eventData.primaryFysiskBrukersted()),
                eventData.fysiskeBrukersteder()
        );


        inputTopic.pipeInput(UUID.randomUUID().toString(), event);
        inputTopic.pipeInput(UUID.randomUUID().toString(), personopplysningerEndretEvent);


        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(event.getFid(), expectedOpprettet),
                        KeyValue.pair(event.getFid(), expectedEndret)
                );
    }

    @Test
    @DisplayName("RessursTekniskSlettetEventV1 lager tombstone/null RessursState i output topic")
    public void test41() {
        String fid = UUID.randomUUID().toString();

        final RessursOpprettetEventV1.RessursOpprettetEventData eventData = new RessursOpprettetEventV1.RessursOpprettetEventData(
                "personident",
                Sektor.NAV_STATLIG,
                new Visningsnavn(
                        "fornavn",
                        null,
                        "etternavn"
                ),
                List.of("2011"),
                "2011"
        );

        final RessursOpprettetEventV1.RessursOpprettetAdditionalData additionalData = new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
                Optional.of("navident"),
                null,
                null,
                null,
                Optional.of(mockToday),
                Optional.empty(),
                Optional.empty()
        );

        final RessursEvent opprettEvent = new RessursOpprettetEventV1(
                fid,
                eventData,
                (Event.Metadata) null,
                additionalData
        );

        final RessursState expectedOpprettet = new RessursState(
                fid,
                eventData.personident(),
                additionalData.navident().get(),
                eventData.sektor(),
                new Visningsnavn(
                        "fornavn",
                        Optional.empty(),
                        "etternavn"
                ),
                Optional.empty(),
                Optional.of(mockToday),
                Optional.ofNullable(eventData.primaryFysiskBrukersted()),
                eventData.fysiskeBrukersteder()
        );

        final RessursEvent slettEvent = new RessursTekniskSlettetEventV1(
                fid,
                null,
                (Event.Metadata) null
        );

        inputTopic.pipeInput(UUID.randomUUID().toString(), opprettEvent);
        inputTopic.pipeInput(UUID.randomUUID().toString(), slettEvent);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(fid, expectedOpprettet),
                        KeyValue.pair(fid, null)
                );
    }

    @Test
    @DisplayName("Håndterer fysisk brukersted er null for eldre opprettet events")
    public void test42() {
        String fid = UUID.randomUUID().toString();

        final RessursOpprettetEventV1.RessursOpprettetEventData eventData = new RessursOpprettetEventV1.RessursOpprettetEventData(
                "personident",
                Sektor.NAV_STATLIG,
                new Visningsnavn(
                        "fornavn",
                        null,
                        "etternavn"
                ),
                Collections.emptyList(),
                null
        );

        final RessursOpprettetEventV1.RessursOpprettetAdditionalData additionalData = new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
                Optional.of("navident"),
                null,
                null,
                null,
                Optional.of(mockToday),
                Optional.empty(),
                Optional.empty()
        );

        final RessursEvent opprettEvent = new RessursOpprettetEventV1(
                fid,
                eventData,
                (Event.Metadata) null,
                additionalData
        );

        final RessursState expectedOpprettet = new RessursState(
                fid,
                eventData.personident(),
                additionalData.navident().get(),
                eventData.sektor(),
                new Visningsnavn(
                        "fornavn",
                        Optional.empty(),
                        "etternavn"
                ),
                Optional.empty(),
                Optional.of(mockToday),
                Optional.empty(),
                Collections.emptyList()
        );

        inputTopic.pipeInput(UUID.randomUUID().toString(), opprettEvent);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(fid, expectedOpprettet)
                );
    }

    private void setupKafkaTransformer() {
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

    private static ObjectMapper configureObjectMapper() {
        final var objectMapper = new ObjectMapper();

        return objectMapper.findAndRegisterModules()
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
}
