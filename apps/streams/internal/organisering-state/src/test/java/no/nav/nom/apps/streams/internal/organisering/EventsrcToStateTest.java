package no.nav.nom.apps.streams.internal.organisering;

import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.OrganiseringAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringEndretNavnEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.contract.nom.eventsource.OrganiseringOpprettetEventV1;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class EventsrcToStateTest {
    private final JsonMapper jsonMapper = configureJsonMapper();
    private TestInputTopic<String, OrganiseringEvent> inputTopic;
    private TestOutputTopic<String, OrganiseringState> outputTopic;

    @BeforeEach
    public void setup() {
        setupKafkaTransformer();
    }

    @Test
    @DisplayName("OrganiseringOpprettetEventV1 oppretter OrganiseringState i output topic")
    public void test_opprett_OrganiseringState_i_output_topic() {
        //arrange
        final OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData eventData = getOpprettetEventData();
        final OrganiseringEvent event = getOpprettetOrganiseringEvent(eventData);
        final OrganiseringState expected = new OrganiseringState(
            event.getFid(),
            eventData.id(),
            eventData.navn(),
            eventData.idOver(),
            eventData.navnOver(),
            eventData.gyldigFom(),
            eventData.gyldigTom()
        );

        //act
        inputTopic.pipeInput(UUID.randomUUID().toString(), event);

        //assert
        Assertions.assertThat(outputTopic.readValue()).isEqualTo(expected);
        Assertions.assertThat(outputTopic.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("OrganiseringEndretEventV1 endre OrganiseringState i output topic")
    public void test_endre_OrganiseringState_i_output_topic() {
        //arrange
        final OrganiseringEndretEventV1.OrganiseringEndretEventData eventData = getEndretEventData();
        final OrganiseringEvent event = getEndretOrganiseringEvent(eventData);
        final OrganiseringState expected = new OrganiseringState(
            event.getFid(),
            eventData.id(),
            eventData.navn(),
            eventData.idOver(),
            eventData.navnOver(),
            eventData.gyldigFom(),
            eventData.gyldigTom()
        );

        //act
        inputTopic.pipeInput(event);

        //assert
        Assertions.assertThat(outputTopic.readValue()).isEqualTo(expected);
        Assertions.assertThat(outputTopic.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Organisering Opprettet og Endret i OrganiseringState output topic")
    public void test_opprett_og_endre_OrganiseringState_i_output_topic() {
        // opprett event
        final OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData eventDataOpprett = getOpprettetEventData();
        final OrganiseringEvent eventOpprett = getOpprettetOrganiseringEvent(eventDataOpprett);
        final OrganiseringState expectedOpprett = new OrganiseringState(
            eventOpprett.getFid(),
            eventDataOpprett.id(),
            eventDataOpprett.navn(),
            eventDataOpprett.idOver(),
            eventDataOpprett.navnOver(),
            eventDataOpprett.gyldigFom(),
            eventDataOpprett.gyldigTom()
        );

        // endret event
        final OrganiseringEndretEventV1.OrganiseringEndretEventData eventDataEndre = getEndretEventData();
        final OrganiseringEvent eventEndre = getEndretOrganiseringEvent(eventDataEndre);
        final OrganiseringState expectedEndre = new OrganiseringState(
            eventEndre.getFid(),
            eventDataEndre.id(),
            eventDataEndre.navn(),
            eventDataEndre.idOver(),
            eventDataEndre.navnOver(),
            eventDataEndre.gyldigFom(),
            eventDataEndre.gyldigTom()
        );

        final List<OrganiseringEvent> inputList = Arrays.asList(eventOpprett, eventEndre);
        final List<OrganiseringState> expectedList = Arrays.asList(expectedOpprett, expectedEndre);
        inputTopic.pipeValueList(inputList);

        //assert
        Assertions.assertThat(outputTopic.readValuesToList()).hasSameElementsAs(expectedList);
        Assertions.assertThat(outputTopic.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("OrganiseringAvsluttet event setter gyldigTom i state")
    void OrganiseringAvsluttetEvent_sets_gyldigTom_in_state_topic() {
        final OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData eventDataOpprett = getOpprettetEventData();
        final OrganiseringEvent eventOpprett = getOpprettetOrganiseringEvent(eventDataOpprett);

        final OrganiseringAvsluttetEventV1.OrganiseringAvsluttetEventData eventDataAvslutt = getAvsluttEventData(LocalDate.now());
        final OrganiseringEvent eventAvslutt = getAvsluttOrganiseringEvent(eventOpprett.getFid(), eventDataAvslutt);
        final OrganiseringState expectedAvslutt = new OrganiseringState(
            eventOpprett.getFid(),
            eventDataOpprett.id(),
            eventDataOpprett.navn(),
            eventDataOpprett.idOver(),
            eventDataOpprett.navnOver(),
            eventDataOpprett.gyldigFom(),
            LocalDate.now()
        );

        final List<OrganiseringEvent> inputList = Arrays.asList(eventOpprett, eventAvslutt);
        inputTopic.pipeValueList(inputList);

        Assertions.assertThat(outputTopic.readValuesToList()).contains(expectedAvslutt);
        Assertions.assertThat(outputTopic.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("OrganiseringEndretNavn event endrer navn i State")
    void OrganiseringEndretEvent_endrer_navn_i_state_topic() {
        final OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData eventDataOpprett = getOpprettetEventData();
        final OrganiseringEvent eventOpprett = getOpprettetOrganiseringEvent(eventDataOpprett);

        final OrganiseringEndretNavnEventV1.OrganiseringEndretNavnEventData endretNavnEventData = getEndretNavnEventData(eventDataOpprett.id(), "navn 2.0");
        final OrganiseringEvent navnEndret = getEndretNavnEvent(eventOpprett.getFid(), endretNavnEventData);

        final OrganiseringState expectedNavnEndret = new OrganiseringState(
            eventOpprett.getFid(),
            eventDataOpprett.id(),
            endretNavnEventData.navn(),
            eventDataOpprett.idOver(),
            eventDataOpprett.navnOver(),
            eventDataOpprett.gyldigFom(),
            eventDataOpprett.gyldigTom()
        );

        final List<OrganiseringEvent> inputList = Arrays.asList(eventOpprett, navnEndret);
        inputTopic.pipeValueList(inputList);

        Assertions.assertThat(outputTopic.readValuesToList()).contains(expectedNavnEndret);
        Assertions.assertThat(outputTopic.isEmpty()).isTrue();
    }

    private void setupKafkaTransformer() {
        KafkaConfig kafkaConfig = new KafkaConfig();
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final var objectSerde = kafkaConfig.objectSerde(jsonMapper);

        new EventsrcToState(
            "inputTopic",
            "outputTopic",
            jsonMapper,
            objectSerde,
            streamsBuilder
        );

        Topology topology = streamsBuilder.build();

        @SuppressWarnings("resource")
        TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology, new Properties());

        inputTopic = topologyTestDriver.createInputTopic("inputTopic", new StringSerializer(), objectSerde.copyWithType(OrganiseringEvent.class).serializer());

        final var organiseringStateSerde = objectSerde.copyWithType(OrganiseringState.class);
        organiseringStateSerde.deserializer().addTrustedPackages(OrganiseringState.class.getPackageName());
        outputTopic = topologyTestDriver.createOutputTopic("outputTopic", new StringDeserializer(), organiseringStateSerde.deserializer());
    }

    private static JsonMapper configureJsonMapper() {
        return new JsonMapper();
    }


    private OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData getOpprettetEventData() {
        return new OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData(
            "id",
            "navn",
            "idOver",
            "navnOver",
            LocalDate.of(2021, 1, 1),
            null
        );
    }

    private OrganiseringEvent getOpprettetOrganiseringEvent(OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData eventData) {
        return new OrganiseringOpprettetEventV1(
            UUID.randomUUID().toString(),
            eventData,
            (Event.Metadata) null
        );
    }

    private OrganiseringEndretEventV1.OrganiseringEndretEventData getEndretEventData() {
        return new OrganiseringEndretEventV1.OrganiseringEndretEventData(
            "id",
            "navn",
            "idOver",
            "navnOver",
            LocalDate.of(2021, 1, 1),
            null
        );
    }

    private OrganiseringEvent getEndretOrganiseringEvent(OrganiseringEndretEventV1.OrganiseringEndretEventData eventData) {
        return new OrganiseringEndretEventV1(
            UUID.randomUUID().toString(),
            eventData,
            (Event.Metadata) null
        );
    }

    private OrganiseringEvent getAvsluttOrganiseringEvent(String fid, OrganiseringAvsluttetEventV1.OrganiseringAvsluttetEventData eventData) {
        return new OrganiseringAvsluttetEventV1(
            fid,
            eventData,
            (Event.Metadata) null
        );
    }

    private OrganiseringAvsluttetEventV1.OrganiseringAvsluttetEventData getAvsluttEventData(LocalDate gyldigTom) {
        return new OrganiseringAvsluttetEventV1.OrganiseringAvsluttetEventData(
            gyldigTom
        );
    }

    private OrganiseringEvent getEndretNavnEvent(String fid, OrganiseringEndretNavnEventV1.OrganiseringEndretNavnEventData eventData) {
        return new OrganiseringEndretNavnEventV1(
            fid,
            eventData,
            (Event.Metadata) null
        );
    }

    private OrganiseringEndretNavnEventV1.OrganiseringEndretNavnEventData getEndretNavnEventData(String id, String navn) {
        return new OrganiseringEndretNavnEventV1.OrganiseringEndretNavnEventData(
            id,
            navn
        );
    }

}


