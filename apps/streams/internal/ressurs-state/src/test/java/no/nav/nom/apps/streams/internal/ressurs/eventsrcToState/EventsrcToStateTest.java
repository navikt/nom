package no.nav.nom.apps.streams.internal.ressurs.eventsrcToState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursFysiskeBrukerstederOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursJobbtelefonOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public class EventsrcToStateTest {
    private ObjectMapper objectMapper = configureObjectMapper();
    private TestInputTopic<String, RessursEvent> inputTopic;
    private TestInputTopic<String, String> identTopic;
    private TestOutputTopic<String, RessursState> outputTopic;

    private static ObjectMapper configureObjectMapper() {
        final var objectMapper = new ObjectMapper();

        return objectMapper.findAndRegisterModules()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @BeforeEach
    public void setup() {
        setupKafkaTransformer();
    }

    @Test
    @DisplayName("RessursOpprettetEventV1 oppretter RessursState i output topic")
    public void test11() {

        identTopic.pipeInput(navidentToIdentTypeObject("navident", IdentType.STANDARD));
        final RessursState expectedOpprettetRessurs = opprettRessurs();

        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(expectedOpprettetRessurs.fid(), expectedOpprettetRessurs)
            );
    }

    private String navidentToIdentTypeObject(String navident, IdentType identType) {
        var payload = objectMapper.createObjectNode().put("navident", navident).put("identType", identType.toString());
        return objectMapper.createObjectNode().putPOJO("payload", payload).toString();
    }

    @Test
    @DisplayName("Merger RessursSluttet event merges sammen inn i state")
    public void test21() {

        identTopic.pipeInput(navidentToIdentTypeObject("navident", IdentType.STANDARD));
        final RessursState opprettetRessurs = opprettRessurs();

        final RessursSluttetEventV1.RessursSluttetEventData sluttetEventData = new RessursSluttetEventV1.RessursSluttetEventData(LocalDate.now());

        final RessursEvent sluttetEvent = new RessursSluttetEventV1(
            opprettetRessurs.fid(),
            sluttetEventData,
            (Event.Metadata) null
        );

        final RessursState expectedSluttet = opprettetRessurs.toBuilder()
            .sluttdato(sluttetEventData.sluttdato())
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

        identTopic.pipeInput(navidentToIdentTypeObject("navident", IdentType.STANDARD));
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

        identTopic.pipeInput(navidentToIdentTypeObject("navident", IdentType.STANDARD));
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
            null,
            Collections.emptyList(),
            null,
            IdentType.STANDARD
        );

        final RessursOpprettetEventV1.RessursOpprettetAdditionalData additionalData = new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
            "navident",
            null,
            null,
            null,
            LocalDate.now(),
            null,
            null
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
            additionalData.navident(),
            eventData.sektor(),
            new Visningsnavn(
                "fornavn",
                Optional.empty(),
                "etternavn"
            ),
            null,
            null,
            null,
            additionalData.startdato(),
            null,
            Collections.emptyList(),
            IdentType.STANDARD,
            null
        );

        identTopic.pipeInput(navidentToIdentTypeObject("navident", IdentType.STANDARD));
        inputTopic.pipeInput(UUID.randomUUID().toString(), opprettEvent);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(fid, expectedOpprettet)
            );
    }

    @Test
    @DisplayName("Merger RessursFysiskeBrukerstederOppdatertEventV1 event merges sammen inn i state")
    public void testRessursFysiskeBrukerstederOppdatertEventV1() {

        identTopic.pipeInput(navidentToIdentTypeObject("navident", IdentType.STANDARD));
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
            .primaryFysiskBrukersted(ressursFysiskeBrukerstederOppdatertEventData.primaryFysiskBrukersted())
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

        identTopic.pipeInput(navidentToIdentTypeObject("navident", IdentType.STANDARD));
        final RessursState opprettetRessurs = opprettRessurs();

        final RessursSluttdatoOppdatertEventV1.RessursSluttdatoOppdatertEventData ressursSluttdatoOppdatertEventData =
            new RessursSluttdatoOppdatertEventV1.RessursSluttdatoOppdatertEventData(
                LocalDate.now().plusYears(1)
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

    @Test
    @DisplayName("Håndterer RessursJobbtelefonOppdatertEventV1 event merges sammen inn i state")
    void testRessursJobbtelefonOppdatertEventV1() {

        final RessursState opprettetRessurs = opprettRessurs();

        final RessursJobbtelefonOppdatertEventV1.JobbtelefonOppdatertEventData ressursJobbtelefonOppdatertEventV1Data = new RessursJobbtelefonOppdatertEventV1.JobbtelefonOppdatertEventData(
            opprettetRessurs.navident(),
            new Jobbtelefon("12345678",
            TelefonType.NAV_TJENESTE_TELEFON)
        );

        final RessursEvent ressursJobbtelefonOppdatertEventV1 = new RessursJobbtelefonOppdatertEventV1(
            opprettetRessurs.fid(),
            ressursJobbtelefonOppdatertEventV1Data,
            (Event.Metadata) null
        );

        final RessursState expectedEndret = opprettetRessurs.toBuilder()
            .jobbtelefon(new Jobbtelefon("12345678", TelefonType.NAV_TJENESTE_TELEFON))
            .build();

        inputTopic.pipeInput(UUID.randomUUID().toString(), ressursJobbtelefonOppdatertEventV1);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(opprettetRessurs.fid(), opprettetRessurs),
                KeyValue.pair(opprettetRessurs.fid(), expectedEndret)
            );
    }

    @Test
    @DisplayName("Håndterer RessursJobbtelefonOppdatertEventV1 event som er null merges sammen inn i state")
    void testRessursJobbtelefonOppdatertEventV1Slettes() {

        final RessursState opprettetRessurs = opprettRessursMedJobbtelefon();

        final RessursJobbtelefonOppdatertEventV1.JobbtelefonOppdatertEventData ressursJobbtelefonOppdatertEventV1Data = new RessursJobbtelefonOppdatertEventV1.JobbtelefonOppdatertEventData(
            opprettetRessurs.navident(),
            null
        );

        final RessursEvent ressursJobbtelefonOppdatertEventV1 = new RessursJobbtelefonOppdatertEventV1(
            opprettetRessurs.fid(),
            ressursJobbtelefonOppdatertEventV1Data,
            (Event.Metadata) null
        );

        final RessursState expectedEndret = opprettetRessurs.toBuilder()
            .jobbtelefon(null)
            .build();

        inputTopic.pipeInput(UUID.randomUUID().toString(), ressursJobbtelefonOppdatertEventV1);

        var output = outputTopic.readKeyValuesToList();

        Assertions.assertThat(output)
            .containsExactly(
                KeyValue.pair(opprettetRessurs.fid(), opprettetRessurs.toBuilder().jobbtelefon(null).build()),
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
            null,
            List.of("2011"),
            "2011",
            IdentType.STANDARD
        );

        final RessursOpprettetEventV1.RessursOpprettetAdditionalData ressursOpprettetAdditionalData = new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
           "navident",
            null,
            null,
            null,
            LocalDate.now(),
            null,
            null
        );

        final RessursEvent event = new RessursOpprettetEventV1(
            UUID.randomUUID().toString(),
            ressursOpprettetEventData,
            (Event.Metadata) null,
            ressursOpprettetAdditionalData

        );

        identTopic.pipeInput(navidentToIdentTypeObject("navident", IdentType.STANDARD));
        inputTopic.pipeInput(UUID.randomUUID().toString(), event);

        return new RessursState(
            event.getFid(),
            ressursOpprettetEventData.personident(),
            ressursOpprettetAdditionalData.navident(),
            ressursOpprettetEventData.sektor(),
            ressursOpprettetEventData.visningsnavn(),
            null,
            null,
            null,
            ressursOpprettetAdditionalData.startdato(),
            ressursOpprettetEventData.primaryFysiskBrukersted(),
            ressursOpprettetEventData.fysiskeBrukersteder(),
            IdentType.STANDARD,
            null
        );
    }

    private RessursState opprettRessursMedJobbtelefon() {

        final RessursOpprettetEventV1.RessursOpprettetEventData ressursOpprettetEventData = new RessursOpprettetEventV1.RessursOpprettetEventData(
            "personident",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "fornavn",
                Optional.empty(),
                "etternavn"
            ),
            null,
            List.of("2011"),
            "2011",
            IdentType.STANDARD
        );

        final RessursOpprettetEventV1.RessursOpprettetAdditionalData ressursOpprettetAdditionalData = new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
            "navident",
            null,
            null,
            null,
            LocalDate.now(),
            null,
            null
        );

        final RessursEvent event = new RessursOpprettetEventV1(
            UUID.randomUUID().toString(),
            ressursOpprettetEventData,
            (Event.Metadata) null,
            ressursOpprettetAdditionalData

        );

        identTopic.pipeInput(navidentToIdentTypeObject("navident", IdentType.STANDARD));
        inputTopic.pipeInput(UUID.randomUUID().toString(), event);

        final RessursJobbtelefonOppdatertEventV1.JobbtelefonOppdatertEventData ressursJobbtelefonOppdatertEventV1Data = new RessursJobbtelefonOppdatertEventV1.JobbtelefonOppdatertEventData(
            ressursOpprettetAdditionalData.navident(),
            new Jobbtelefon("12345678",
                TelefonType.NAV_TJENESTE_TELEFON)
        );

        final RessursEvent ressursJobbtelefonOppdatertEventV1 = new RessursJobbtelefonOppdatertEventV1(
            event.getFid(),
            ressursJobbtelefonOppdatertEventV1Data,
            (Event.Metadata) null
        );

        inputTopic.pipeInput(UUID.randomUUID().toString(), ressursJobbtelefonOppdatertEventV1);

        return new RessursState(
            event.getFid(),
            ressursOpprettetEventData.personident(),
            ressursOpprettetAdditionalData.navident(),
            ressursOpprettetEventData.sektor(),
            ressursOpprettetEventData.visningsnavn(),
            new Jobbtelefon("12345678", TelefonType.NAV_TJENESTE_TELEFON),
            null,
            null,
            ressursOpprettetAdditionalData.startdato(),
            ressursOpprettetEventData.primaryFysiskBrukersted(),
            ressursOpprettetEventData.fysiskeBrukersteder(),
            IdentType.STANDARD,
            null
        );
    }

    private void setupKafkaTransformer() {
        KafkaConfig kafkaConfig = new KafkaConfig();
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final var objectSerde = kafkaConfig.objectSerde(objectMapper);
        final var jsonSerde = kafkaConfig.jsonSerde(objectMapper);
        //        kafkaConfig.

        new EventsrcToState(
            "inputTopic",
            "outputTopic",
            "identTopic",
            objectMapper,
            objectSerde,
            jsonSerde,
            streamsBuilder
        );

        Topology topology = streamsBuilder.build();

        TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology, new Properties());

        inputTopic = topologyTestDriver.createInputTopic("inputTopic", new StringSerializer(), objectSerde.copyWithType(RessursEvent.class).serializer());
        identTopic = topologyTestDriver.createInputTopic("identTopic", new StringSerializer(), new StringSerializer());

        final var ressursStateSerde = objectSerde.copyWithType(RessursState.class);
        ressursStateSerde.deserializer().addTrustedPackages(RessursState.class.getPackageName());
        outputTopic = topologyTestDriver.createOutputTopic("outputTopic", new StringDeserializer(), ressursStateSerde.deserializer());
    }
}
