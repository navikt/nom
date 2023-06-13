package no.nav.nom.apps.streams.internal.orgtilknytning.eventsrctostate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDatoEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFjernetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
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

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Properties;

public class KafkaProcessorTest {
    private final ObjectMapper objectMapper = configureObjectMapper();
    private TestInputTopic<String, OrgtilknytningEvent> inputTopic;
    private TestOutputTopic<String, OrgtilknytningState> outputTopic;

    @BeforeEach
    public void setup(){
        setupKafkaTransformer();
    }

    @Test
    @DisplayName("OrgtilknytningOpprettetEventV1 oppretter OrgtilknytningState i output topic")
    public void test_001(){
        String navident = "A000001";
        String orgEnhetId = "'123bc";
        LocalDate gyldigFom = LocalDate.now();
        
        final OrgtilknytningEvent event = lagOpprettEvent(navident, orgEnhetId, gyldigFom);

        final OrgtilknytningState expected = lagStateObjekt(navident, orgEnhetId, false, gyldigFom, null);

        inputTopic.pipeInput(event.getFid(), event);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(event.getFid(), expected)
                );
    }

    @Test
    @DisplayName("Merger OrgtilknytningOpprettet og OrgtilknytningAvsluttet event sammen inn i state")
    public void test_002(){
        String navident = "A000001";
        String orgEnhetId = "'123bc";
        LocalDate gyldigFom = LocalDate.of(2022, 1,  1);
        LocalDate gyldigTom = LocalDate.of(2022, 12, 31);

        final OrgtilknytningEvent opprettEvent = lagOpprettEvent(navident, orgEnhetId, gyldigFom);
        final OrgtilknytningEvent avsluttetEvent = lagAvsluttEvent(navident, orgEnhetId, gyldigTom);

        final OrgtilknytningState expectedOpprettet = lagStateObjekt(
            navident,
            orgEnhetId,
            false,
            gyldigFom,
            null
        );

        final OrgtilknytningState expectedAvsluttet = lagStateObjekt(
            navident,
            orgEnhetId,
            false,
            gyldigFom,
            gyldigTom
        );


        inputTopic.pipeInput(opprettEvent.getFid(), opprettEvent);
        inputTopic.pipeInput(opprettEvent.getFid(), avsluttetEvent);


        Assertions.assertThat(outputTopic.readKeyValuesToList())
                .containsExactly(
                        KeyValue.pair(opprettEvent.getFid(), expectedOpprettet),
                        KeyValue.pair(opprettEvent.getFid(), expectedAvsluttet)
                );
    }

    @Test
    @DisplayName("Merger OrgtilknytningOpprettet, OrgtilknytningDagligOppfølgingEndret og OrgtilknytningAvsluttet eventer sammen inn i state")
    public void test_003(){
        String navident = "A000001";
        String orgEnhetId = "'123bc";
        LocalDate gyldigFom = LocalDate.of(2022, 1,  1);
        LocalDate gyldigTom = LocalDate.of(2022, 12, 31);

        final OrgtilknytningEvent opprettEvent = lagOpprettEvent(navident, orgEnhetId, gyldigFom);
        final OrgtilknytningEvent dagligOppfolgingEndretEvent = lagDagligOppfolgingEndretEvent(navident, orgEnhetId, gyldigFom, null, true);
        final OrgtilknytningEvent avsluttetEvent = lagAvsluttEvent(navident, orgEnhetId, gyldigTom);

        final OrgtilknytningState expectedOpprettet = lagStateObjekt(
            navident,
            orgEnhetId,
            false,
            gyldigFom,
            null
        );

        final OrgtilknytningState expectedDagligOppforingEndret = lagStateObjekt(
            navident,
            orgEnhetId,
            true,
            gyldigFom,
            null
        );

        final OrgtilknytningState expectedAvsluttet = lagStateObjekt(
            navident,
            orgEnhetId,
            true,
            gyldigFom,
            gyldigTom
        );


        inputTopic.pipeInput(opprettEvent.getFid(), opprettEvent);
        inputTopic.pipeInput(opprettEvent.getFid(), dagligOppfolgingEndretEvent);
        inputTopic.pipeInput(opprettEvent.getFid(), avsluttetEvent);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(opprettEvent.getFid(), expectedOpprettet),
                KeyValue.pair(opprettEvent.getFid(), expectedDagligOppforingEndret),
                KeyValue.pair(opprettEvent.getFid(), expectedAvsluttet)
            );
    }

    @Test
    @DisplayName("Lag orgtilknytning, sett avsluttet, endre datoer, og til sist fjern den")
    public void test_004(){
        String navident = "A000001";
        String orgEnhetId = "'123bc";
        LocalDate gyldigFom = LocalDate.of(2022, 1,  1);
        LocalDate gyldigTom = LocalDate.of(2022, 12, 31);
        LocalDate nyGyldigFom = LocalDate.of(2023, 1,  1);
        LocalDate nyGyldigTom = LocalDate.of(2023, 12, 31);

        final OrgtilknytningEvent opprettEvent = lagOpprettEvent(navident, orgEnhetId, gyldigFom);
        final OrgtilknytningEvent avsluttetEvent = lagAvsluttEvent(navident, orgEnhetId, gyldigTom);
        final OrgtilknytningEvent endretDatoEvent = lagDatoEndretEvent(navident, orgEnhetId, nyGyldigFom, nyGyldigTom);
        final OrgtilknytningEvent fjernetEvent = lagFjernetEvent(navident, orgEnhetId, nyGyldigFom, nyGyldigTom);

        final OrgtilknytningState expectedOpprettet = lagStateObjekt(
            navident,
            orgEnhetId,
            false,
            gyldigFom,
            null
        );

        final OrgtilknytningState expectedAvsluttet = lagStateObjekt(
            navident,
            orgEnhetId,
            false,
            gyldigFom,
            gyldigTom
        );

        final OrgtilknytningState expectedEndret = lagStateObjekt(
            navident,
            orgEnhetId,
            false,
            nyGyldigFom,
            nyGyldigTom
        );

        inputTopic.pipeInput(opprettEvent.getFid(), opprettEvent);
        inputTopic.pipeInput(opprettEvent.getFid(), avsluttetEvent);
        inputTopic.pipeInput(opprettEvent.getFid(), endretDatoEvent);
        inputTopic.pipeInput(opprettEvent.getFid(), fjernetEvent);


        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(opprettEvent.getFid(), expectedOpprettet),
                KeyValue.pair(opprettEvent.getFid(), expectedAvsluttet),
                KeyValue.pair(opprettEvent.getFid(), expectedEndret),
                KeyValue.pair(opprettEvent.getFid(), null)
            );
    }


    @Test
    @DisplayName("Send kun en Fjern orgtilknytning event")
    public void test_005(){
        String navident = "A000001";
        String orgEnhetId = "'123bc";
        LocalDate gyldigFom = LocalDate.of(2022, 1,  1);
        LocalDate gyldigTom = LocalDate.of(2022, 12, 31);

        final OrgtilknytningEvent fjernetEvent = lagFjernetEvent(navident, orgEnhetId, gyldigFom, gyldigTom);


        inputTopic.pipeInput(fjernetEvent.getFid(), fjernetEvent);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(fjernetEvent.getFid(), null)
            );
    }

    private OrgtilknytningOpprettetEventV1 lagOpprettEvent(String navident, String orgEnhetId, LocalDate gyldigFom) {
        return lagOpprettEvent("FID_1", navident, orgEnhetId, gyldigFom);
    }

    private OrgtilknytningOpprettetEventV1 lagOpprettEvent(String fid, String navident, String orgEnhetId, LocalDate gyldigFom) {
        return new OrgtilknytningOpprettetEventV1(
            fid,
            new OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData(
                navident,
                orgEnhetId,
                gyldigFom
            ),
            (Event.Metadata) null,
            dummyAdditionalAgressoData
        );
    }

    private OrgtilknytningAvsluttetEventV1 lagAvsluttEvent(String navident, String orgEnhetId, LocalDate gyldigTom) {
        return lagAvsluttEvent("FID_1", navident, orgEnhetId, gyldigTom);
    }

    private OrgtilknytningAvsluttetEventV1 lagAvsluttEvent(String fid, String navident, String orgEnhetId, LocalDate gyldigTom) {
        return new OrgtilknytningAvsluttetEventV1(
            fid,
            new OrgtilknytningAvsluttetEventV1.OrgtilknytningAvsluttetEventData(
                navident,
                orgEnhetId,
                gyldigTom
            ),
            (Event.Metadata) null,
            dummyAdditionalAgressoData
        );
    }

    private OrgtilknytningDagligOppfolgingEndretEventV1 lagDagligOppfolgingEndretEvent(String navident, String orgEnhetId, LocalDate gyldigFom, LocalDate gyldigTom, boolean dagligOppfolging) {
        return lagDagligOppfolgingEndretEvent("FID_1", navident, orgEnhetId, gyldigFom, gyldigTom, dagligOppfolging);
    }

    private OrgtilknytningDagligOppfolgingEndretEventV1 lagDagligOppfolgingEndretEvent(String fid, String navident, String orgEnhetId, LocalDate gyldigFom, LocalDate gyldigTom, boolean dagligOppfolging) {
        return new OrgtilknytningDagligOppfolgingEndretEventV1(
            fid,
            new OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData(
                navident,
                orgEnhetId,
                gyldigFom,
                Optional.ofNullable(gyldigTom),
                dagligOppfolging
            ),
            (Event.Metadata) null,
            dummyAdditionalAgressoData
        );
    }

    private OrgtilknytningDatoEndretEventV1 lagDatoEndretEvent(String navident, String orgEnhetId, LocalDate gyldigFom, LocalDate gyldigTom) {
        return lagDatoEndretEvent("FID_1", navident, orgEnhetId, gyldigFom, gyldigTom);
    }

    private OrgtilknytningDatoEndretEventV1 lagDatoEndretEvent(String fid, String navident, String orgEnhetId, LocalDate gyldigFom, LocalDate gyldigTom) {
        return new OrgtilknytningDatoEndretEventV1(
            fid,
            new OrgtilknytningDatoEndretEventV1.OrgtilknytningDatoEndretEventData(
                navident,
                orgEnhetId,
                gyldigFom,
                Optional.ofNullable(gyldigTom)
            ),
            (Event.Metadata) null,
            dummyAdditionalAgressoData
        );
    }

    private OrgtilknytningFjernetEventV1 lagFjernetEvent(String navident, String orgEnhetId, LocalDate gyldigFom, LocalDate gyldigTom) {
        return lagFjernetEvent("FID_1", navident, orgEnhetId, gyldigFom, gyldigTom);
    }

    private OrgtilknytningFjernetEventV1 lagFjernetEvent(String fid, String navident, String orgEnhetId, LocalDate gyldigFom, LocalDate gyldigTom) {
        return new OrgtilknytningFjernetEventV1(
            fid,
            new OrgtilknytningFjernetEventV1.OrgtilknytningFjernetEventData(
                navident,
                orgEnhetId,
                gyldigFom,
                Optional.ofNullable(gyldigTom)
            ),
            (Event.Metadata) null,
            dummyAdditionalAgressoData
        );
    }

    private static OrgtilknytningState lagStateObjekt(String navident, String orgEnhetId, boolean erDagligOppfolging, LocalDate gyldigFom, LocalDate gyldigTom) {
        return lagStateObjekt("FID_1", navident, orgEnhetId, erDagligOppfolging, gyldigFom, gyldigTom);
    }

    private static OrgtilknytningState lagStateObjekt(String fid, String navident, String orgEnhetId, boolean erDagligOppfolging, LocalDate gyldigFom, LocalDate gyldigTom) {
        return new OrgtilknytningState(
            fid,
            navident,
            orgEnhetId,
            erDagligOppfolging,
            gyldigFom,
            Optional.ofNullable(gyldigTom)
        );
    }


    final OrgtilknytningEvent.AdditionalAgressoData dummyAdditionalAgressoData =
        new OrgtilknytningEvent.AdditionalAgressoData(
            "dummy_hrresStillingRessursEnhet_String",
            "dummy_hrresStillingerPerioderFormatert_String"
        );
    
    private void setupKafkaTransformer(){
        KafkaConfig kafkaConfig = new KafkaConfig();
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final var objectSerde = kafkaConfig.objectSerde(objectMapper);


        new KafkaStreamProcessor(
                "inputTopic",
                "outputTopic",
                objectMapper,
                objectSerde,
                streamsBuilder
        );

        Topology topology = streamsBuilder.build();

        TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology, new Properties());
        inputTopic = topologyTestDriver.createInputTopic("inputTopic", new StringSerializer(), objectSerde.copyWithType(OrgtilknytningEvent.class).serializer());

        final var orgtilknytningStateSerde = objectSerde.copyWithType(OrgtilknytningState.class);
        orgtilknytningStateSerde.deserializer().addTrustedPackages(OrgtilknytningState.class.getPackageName());
        outputTopic = topologyTestDriver.createOutputTopic("outputTopic", new StringDeserializer(), orgtilknytningStateSerde.deserializer());
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

