package no.nav.nom.apps.streams.internal.orgenehetleder.eventsrctostate;

import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederEvent;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederSluttdatoOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederStartdatoOppdatertEventV1;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
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

public class EventsrcToStateTest {
    private final JsonMapper jsonMapper = configureJsonMapper();
    private TestInputTopic<String, OrgEnhetLederEvent> inputTopic;
    private TestOutputTopic<String, OrgEnhetLederState> outputTopic;

    @BeforeEach
    public void setup() {
        setupKafkaTransformer();
    }

    @Test
    @DisplayName("OrgEnhetLederOpprettetEventV1 oppretter OrgEnhetLederState i output topic")
    void test_001() {
        String navident = "A000001";
        String orgEnhetId = "ab123c";

        final OrgEnhetLederEvent event = lagOpprettetEvent(navident, orgEnhetId);

        final OrgEnhetLederState expected = lagStateObjekt(navident, orgEnhetId, null, null, "nom-api");

        inputTopic.pipeInput(event.getFid(), event);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(event.getFid(), expected)
            );
    }

    @Test
    @DisplayName("Merger OrgEnhetLederOpprettetEventV1 og OrgEnhetLederStartdatoOppdatertV1 event sammen i state")
    void test_002() {
        String navident = "A000001";
        String orgEnhetId = "ab123c";

        final OrgEnhetLederEvent opprettetEvent = lagOpprettetEvent(navident, orgEnhetId);
        final OrgEnhetLederEvent startdatoEvent = lagStartdatoEvent(opprettetEvent.getFid(), LocalDate.now());

        final OrgEnhetLederState expectedOpprettet = lagStateObjekt(navident, orgEnhetId, null, null, "nom-api");

        final OrgEnhetLederState expectedStartdato = lagStateObjekt(navident, orgEnhetId, LocalDate.now(), null, "nom-api");

        inputTopic.pipeInput(opprettetEvent.getFid(), opprettetEvent);
        inputTopic.pipeInput(opprettetEvent.getFid(), startdatoEvent);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(opprettetEvent.getFid(), expectedOpprettet),
                KeyValue.pair(opprettetEvent.getFid(), expectedStartdato)
            );
    }

    @Test
    @DisplayName("Merger OrgEnhetLederOpprettet, StartdatoOppdatert og SluttdatoOppdatertEventV1 eventer sammen inn i state")
    void test_003() {
        String navident = "A000001";
        String orgEnhetId = "ab123c";
        LocalDate startdato = LocalDate.now();
        LocalDate sluttdato = startdato.plusMonths(3);

        final OrgEnhetLederEvent opprettetEvent = lagOpprettetEvent(navident, orgEnhetId);
        final OrgEnhetLederEvent startdatoEvent = lagStartdatoEvent(opprettetEvent.getFid(), startdato);
        final OrgEnhetLederEvent sluttdatoEvent = lagSluttdatoEvent(opprettetEvent.getFid(), sluttdato);

        final OrgEnhetLederState expectedOpprettet = lagStateObjekt(navident, orgEnhetId, null, null, "nom-api");

        final OrgEnhetLederState expectedStartdato = lagStateObjekt(navident, orgEnhetId, startdato, null, "nom-api");

        final OrgEnhetLederState expectedSluttdato = lagStateObjekt(navident, orgEnhetId, startdato, sluttdato, "nom-api");

        inputTopic.pipeInput(opprettetEvent.getFid(), opprettetEvent);
        inputTopic.pipeInput(opprettetEvent.getFid(), startdatoEvent);
        inputTopic.pipeInput(opprettetEvent.getFid(), sluttdatoEvent);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(opprettetEvent.getFid(), expectedOpprettet),
                KeyValue.pair(opprettetEvent.getFid(), expectedStartdato),
                KeyValue.pair(opprettetEvent.getFid(), expectedSluttdato)
            );
    }

    private OrgEnhetLederOpprettetEventV1 lagOpprettetEvent(String navident, String orgEnhetId) {
        return lagOpprettetEvent("FID_001", navident, orgEnhetId);
    }

    private OrgEnhetLederOpprettetEventV1 lagOpprettetEvent(String fid, String navident, String orgEnhetId) {
        return new OrgEnhetLederOpprettetEventV1(
            fid,
            new OrgEnhetLederOpprettetEventV1.OrgEnhetLederOpprettetEventData(
                navident,
                orgEnhetId
            ),
            lagMetadata()
        );
    }

    private OrgEnhetLederStartdatoOppdatertEventV1 lagStartdatoEvent(String fid, LocalDate startdato) {
        return new OrgEnhetLederStartdatoOppdatertEventV1(
            fid,
            new OrgEnhetLederStartdatoOppdatertEventV1.OrgEnhetLederStartdatoOppdatertData(
                startdato
            ),
            lagMetadata()
        );
    }

    private OrgEnhetLederSluttdatoOppdatertEventV1 lagSluttdatoEvent(String fid, LocalDate sluttdato) {
        return new OrgEnhetLederSluttdatoOppdatertEventV1(
            fid,
            new OrgEnhetLederSluttdatoOppdatertEventV1.OrgEnhetLederAvsluttetEventData(
                sluttdato
            ),
            lagMetadata()
        );
    }

    private Event.Metadata lagMetadata() {
        return new Event.Metadata(
            new InputMetadata(
                "test",
                "test",
                null,
                new Origination(
                    "nom-api",
                    "test"
                )
            ),
            LocalDateTime.now()
        );
    }

    private static OrgEnhetLederState lagStateObjekt(String navident, String orgEnhetId, LocalDate startdato, LocalDate sluttdato, String sistEndretAv) {
        return OrgEnhetLederState
            .builder()
            .fid("FID_001")
            .navident(navident)
            .orgEnhetId(orgEnhetId)
            .startdato(startdato)
            .sluttdato(sluttdato)
            .sistEndretAv(sistEndretAv)
            .build();
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
        inputTopic = topologyTestDriver.createInputTopic("inputTopic", new StringSerializer(), objectSerde.copyWithType(OrgEnhetLederEvent.class).serializer());

        final var orgEnhetLederStateSerde = objectSerde.copyWithType(OrgEnhetLederState.class);
        orgEnhetLederStateSerde.deserializer().addTrustedPackages(OrgEnhetLederState.class.getPackageName());
        outputTopic = topologyTestDriver.createOutputTopic("outputTopic", new StringDeserializer(), orgEnhetLederStateSerde.deserializer());
    }


    private static JsonMapper configureJsonMapper() {

        return new JsonMapper();
    }
}
