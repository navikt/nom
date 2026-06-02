package no.nav.nom.apps.streams.internal.orgenhet.eventsrcToState;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.NomNivaaEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
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
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;

public class EventsrcToStateTest {
    private JsonMapper jsonMapper = configureJsonMapper();
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
                "orgEnhet_123",
                "ressurs_123",
                "1234"
        );


        final OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalAgressoData additionalData = new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalAgressoData(
                "AgressoId_123",
                "AgressoNavn",
                "AgressoOrgNivaa_1",
                "AgressoOrgEnhetType",
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
                null,
                eventData.overordnetOrgEnhetFid(),
                eventData.lederRessursFid(),
                null,
                null,
                additionalData.uniqueAgressoId(),
                additionalData.agressoId(),
                additionalData.agressoOrgNivaa(),
                additionalData.agressoOrgenhetType(),
                "1234"
        );

        inputTopic.pipeInput(UUID.randomUUID().toString(), event);


        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(event.getFid(), expected)
                );
    }

    @Test
    @DisplayName("OrgEnhetTypeEndretEventV1 oppdaterer type i output topic")
    public void test12() {
        final var createEventData = new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetEventData(
            "DummyIntenrnId_1",
            "TestName",
            LocalDate.of(2022, 12, 01),
            "orgEnhet_123",
            "ressurs_123",
            "1234"
        );

        final OrgEnhetEvent createEvent = new OrgEnhetOpprettetEventV1(
            UUID.randomUUID().toString(),
            createEventData,
            (Event.Metadata) null,
            null
        );

        inputTopic.pipeInput(UUID.randomUUID().toString(), createEvent);

        final OrgEnhetState expected = new OrgEnhetState(
            createEvent.getFid(),
            createEventData.id(),
            createEventData.navn(),
            createEventData.gyldigFom(),
            null,
            createEventData.overordnetOrgEnhetFid(),
            createEventData.lederRessursFid(),
            null,
            null,
            null,
            null,
            null,
            null,
            "1234"
        );

        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(createEvent.getFid(), expected)
            );

        final OrgEnhetEvent updateTypeEvent = new OrgEnhetTypeEndretEventV1(
            createEvent.getFid(),
            new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(OrgEnhetsType.DIR),
            new Event.Metadata(null, LocalDateTime.now())
        );

        inputTopic.pipeInput(UUID.randomUUID().toString(), updateTypeEvent);

        final OrgEnhetState expectedAfterUpdate = new OrgEnhetState(
            createEvent.getFid(),
            createEventData.id(),
            createEventData.navn(),
            createEventData.gyldigFom(),
            null,
            createEventData.overordnetOrgEnhetFid(),
            createEventData.lederRessursFid(),
            OrgEnhetsType.DIR,
            null,
            null,
            null,
            null,
            null,
            "1234"
        );

        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(updateTypeEvent.getFid(), expectedAfterUpdate)
            );

        final OrgEnhetEvent updateNomNivaaEvent = new NomNivaaEndretEventV1(
            createEvent.getFid(),
            new NomNivaaEndretEventV1.NomNivaaEndretEventData(NomNivaa.ARBEIDSOMRAADE),
            new Event.Metadata(null, LocalDateTime.now())
        );

        inputTopic.pipeInput(UUID.randomUUID().toString(), updateNomNivaaEvent);

        final OrgEnhetState expectedAfterUpdate2 = new OrgEnhetState(
            createEvent.getFid(),
            createEventData.id(),
            createEventData.navn(),
            createEventData.gyldigFom(),
            null,
            createEventData.overordnetOrgEnhetFid(),
            createEventData.lederRessursFid(),
            OrgEnhetsType.DIR,
            NomNivaa.ARBEIDSOMRAADE,
            null,
            null,
            null,
            null,
            "1234"
        );

        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(updateNomNivaaEvent.getFid(), expectedAfterUpdate2)
            );
    }

    private void setupKafkaTransformer(){
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

        TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology, new Properties());

        inputTopic = topologyTestDriver.createInputTopic("inputTopic", new StringSerializer(), objectSerde.copyWithType(OrgEnhetEvent.class).serializer());

        final var orgEnhetStateSerde = objectSerde.copyWithType(OrgEnhetState.class);
        orgEnhetStateSerde.deserializer().addTrustedPackages(OrgEnhetState.class.getPackageName());
        outputTopic = topologyTestDriver.createOutputTopic("outputTopic", new StringDeserializer(), orgEnhetStateSerde.deserializer());
    }

    private static JsonMapper configureJsonMapper() {
        return new JsonMapper();
    }
}
