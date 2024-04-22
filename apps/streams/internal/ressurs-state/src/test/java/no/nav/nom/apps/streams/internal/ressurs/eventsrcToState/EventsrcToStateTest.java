package no.nav.nom.apps.streams.internal.ressurs.eventsrcToState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.RessursFysiskeBrukerstederOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttdatoOppdatertEventV1;
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

    @BeforeEach
    public void setup() {
        setupKafkaTransformer();
    }

    @Test
    @DisplayName("RessursOpprettetEventV1 oppretter RessursState i output topic")
    public void test11() {

        final RessursState expectedOpprettetRessurs = opprettRessurs();

        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(expectedOpprettetRessurs.fid(), expectedOpprettetRessurs)
                );
    }

    @Test
    @DisplayName("Merger RessursSluttet event merges sammen inn i state")
    public void test21() {

        final RessursState opprettetRessurs = opprettRessurs();

        final RessursSluttetEventV1.RessursSluttetEventData sluttetEventData = new RessursSluttetEventV1.RessursSluttetEventData(LocalDate.now());

        final RessursEvent sluttetEvent = new RessursSluttetEventV1(
                opprettetRessurs.fid(),
                sluttetEventData,
                (Event.Metadata) null
        );

        final RessursState expectedSluttet = opprettetRessurs.toBuilder()
            .sluttdato(Optional.of(sluttetEventData.sluttdato()))
            .build();


        inputTopic.pipeInput(UUID.randomUUID().toString(), sluttetEvent);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(opprettetRessurs.fid(), opprettetRessurs),
                        KeyValue.pair(opprettetRessurs.fid(), expectedSluttet)
                );
    }

    @Test
    @DisplayName("Merger PersonopplysningerEndret event merges sammen inn i state")
    public void test31() {

        final RessursState opprettetRessurs = opprettRessurs();

        final RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData personaliaOppdatertEventData = new RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData(
                new Visningsnavn(
                        "fornavnEndret",
                        Optional.empty(),
                        "etternavnEndret"
                )
        );
        final RessursEvent personopplysningerEndretEvent = new RessursPersonaliaOppdatertV1(
                opprettetRessurs.fid(),
                personaliaOppdatertEventData,
                (Event.Metadata) null
        );

        final RessursState expectedEndret = opprettetRessurs.toBuilder()
            .visningsnavn(personaliaOppdatertEventData.visningsnavn())
            .build();

        inputTopic.pipeInput(UUID.randomUUID().toString(), personopplysningerEndretEvent);


        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(opprettetRessurs.fid(), opprettetRessurs),
                        KeyValue.pair(opprettetRessurs.fid(), expectedEndret)
                );
    }

    @Test
    @DisplayName("RessursTekniskSlettetEventV1 lager tombstone/null RessursState i output topic")
    public void test41() {

        final RessursState opprettetRessurs = opprettRessurs();

        final RessursEvent slettEvent = new RessursTekniskSlettetEventV1(
            opprettetRessurs.fid(),
            null,
            (Event.Metadata) null
        );

        inputTopic.pipeInput(UUID.randomUUID().toString(), slettEvent);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(opprettetRessurs.fid(), opprettetRessurs),
                        KeyValue.pair(opprettetRessurs.fid(), null)
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
                Optional.empty(),
                Collections.emptyList(),
                null
        );

        final RessursOpprettetEventV1.RessursOpprettetAdditionalData additionalData = new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
                Optional.of("navident"),
                null,
                null,
                null,
                Optional.of(LocalDate.now()),
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
                Optional.empty(),
                additionalData.startdato(),
                Optional.empty(),
                Collections.emptyList()
        );

        inputTopic.pipeInput(UUID.randomUUID().toString(), opprettEvent);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(fid, expectedOpprettet)
                );
    }

    @Test
    @DisplayName("Merger RessursFysiskeBrukerstederOppdatertEventV1 event merges sammen inn i state")
    public void testRessursFysiskeBrukerstederOppdatertEventV1() {

        final RessursState opprettetRessurs = opprettRessurs();

        final RessursFysiskeBrukerstederOppdatertEventV1.RessursFysiskeBrukerstederOppdatertEventData ressursFysiskeBrukerstederOppdatertEventData =
            RessursFysiskeBrukerstederOppdatertEventV1.RessursFysiskeBrukerstederOppdatertEventData.builder()
                .primaryFysiskBrukersted("oslo")
                .fysiskeBrukersteder(List.of("oslo", "bergen"))
                .build();

        final RessursEvent ressursFysiskeBrukerstederOppdatertEventV1 = new RessursFysiskeBrukerstederOppdatertEventV1(
            opprettetRessurs.fid(),
            ressursFysiskeBrukerstederOppdatertEventData,
            (Event.Metadata) null
        );


        final RessursState expectedEndret = opprettetRessurs.toBuilder()
            .primaryFysiskBrukersted(Optional.ofNullable(ressursFysiskeBrukerstederOppdatertEventData.primaryFysiskBrukersted()))
            .fysiskeBrukersteder(ressursFysiskeBrukerstederOppdatertEventData.fysiskeBrukersteder())
            .build();

        inputTopic.pipeInput(UUID.randomUUID().toString(), ressursFysiskeBrukerstederOppdatertEventV1);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(opprettetRessurs.fid(), opprettetRessurs),
                KeyValue.pair(opprettetRessurs.fid(), expectedEndret)
            );
    }

    @Test
    @DisplayName("Merger RessursSluttdatoOppdatertEventV1 event merges sammen inn i state")
    public void testRessursSluttdatoOppdatertEventV1() {

        final RessursState opprettetRessurs = opprettRessurs();

        final RessursSluttdatoOppdatertEventV1.RessursSluttdatoOppdatertEventData ressursSluttdatoOppdatertEventData =
            new RessursSluttdatoOppdatertEventV1.RessursSluttdatoOppdatertEventData(
                Optional.of(LocalDate.now().plusYears(1))
            );

        final RessursEvent ressursSluttdatoOppdatertEventV1 = new RessursSluttdatoOppdatertEventV1(
            opprettetRessurs.fid(),
            ressursSluttdatoOppdatertEventData,
            (Event.Metadata) null
        );


        final RessursState expectedEndret = opprettetRessurs.toBuilder()
            .sluttdato(ressursSluttdatoOppdatertEventData.sluttdato())
            .build();

        inputTopic.pipeInput(UUID.randomUUID().toString(), ressursSluttdatoOppdatertEventV1);


        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(opprettetRessurs.fid(), opprettetRessurs),
                KeyValue.pair(opprettetRessurs.fid(), expectedEndret)
            );
    }

    private RessursState opprettRessurs() {

        final RessursOpprettetEventV1.RessursOpprettetEventData ressursOpprettetEventData = new RessursOpprettetEventV1.RessursOpprettetEventData(
            "personident",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "fornavn",
                Optional.empty(),
                "etternavn"
            ),
            Optional.empty(),
            List.of("2011"),
            "2011"
        );

        final RessursOpprettetEventV1.RessursOpprettetAdditionalData ressursOpprettetAdditionalData = new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
            Optional.of("navident"),
            null,
            null,
            null,
            Optional.of(LocalDate.now()),
            Optional.empty(),
            Optional.empty()
        );

        final RessursEvent event = new RessursOpprettetEventV1(
            UUID.randomUUID().toString(),
            ressursOpprettetEventData,
            (Event.Metadata) null,
            ressursOpprettetAdditionalData

        );

        inputTopic.pipeInput(UUID.randomUUID().toString(), event);

        return new RessursState(
            event.getFid(),
            ressursOpprettetEventData.personident(),
            ressursOpprettetAdditionalData.navident().get(),
            ressursOpprettetEventData.sektor(),
            ressursOpprettetEventData.visningsnavn(),
            Optional.empty(),
            Optional.empty(),
            ressursOpprettetAdditionalData.startdato(),
            Optional.ofNullable(ressursOpprettetEventData.primaryFysiskBrukersted()),
            ressursOpprettetEventData.fysiskeBrukersteder()
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
