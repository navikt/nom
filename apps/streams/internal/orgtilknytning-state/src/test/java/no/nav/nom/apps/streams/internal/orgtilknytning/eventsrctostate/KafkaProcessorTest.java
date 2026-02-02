package no.nav.nom.apps.streams.internal.orgtilknytning.eventsrctostate;

import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDatoEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFjernetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningTilknytningtypeEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningTypeEndretEventV1;
import no.nav.nom.contract.nom.eventsource.Origination;
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
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

public class KafkaProcessorTest {
    private final JsonMapper jsonMapper = configureJsonMapper();
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
        Sektor sektor = Sektor.NAV_KOMMUNAL;
        LocalDate gyldigFom = LocalDate.now();
        
        final OrgtilknytningEvent event = lagOpprettEvent(navident, orgEnhetId, sektor, gyldigFom);

        final OrgtilknytningState expected = lagStateObjekt(navident, orgEnhetId, sektor,false, gyldigFom, null, "S123456");

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
        Sektor sektor = Sektor.NAV_KOMMUNAL;
        LocalDate gyldigFom = LocalDate.of(2022, 1,  1);
        LocalDate gyldigTom = LocalDate.of(2022, 12, 31);

        final OrgtilknytningEvent opprettEvent = lagOpprettEvent(navident, orgEnhetId, sektor, gyldigFom);
        final OrgtilknytningEvent avsluttetEvent = lagAvsluttEvent(navident, orgEnhetId, gyldigTom);

        final OrgtilknytningState expectedOpprettet = lagStateObjekt(
            navident,
            orgEnhetId,
            sektor,
            false,
            gyldigFom,
            null,
            "S123456"
        );

        final OrgtilknytningState expectedAvsluttet = lagStateObjekt(
            navident,
            orgEnhetId,
            sektor,
            false,
            gyldigFom,
            gyldigTom,
            "S123456"
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
        Sektor sektor = Sektor.EKSTERN;
        LocalDate gyldigFom = LocalDate.of(2022, 1,  1);
        LocalDate gyldigTom = LocalDate.of(2022, 12, 31);

        final OrgtilknytningEvent opprettEvent = lagOpprettEvent(navident, orgEnhetId, sektor, gyldigFom);
        final OrgtilknytningEvent dagligOppfolgingEndretEvent = lagDagligOppfolgingEndretEvent(navident, orgEnhetId, gyldigFom, null, true);
        final OrgtilknytningEvent avsluttetEvent = lagAvsluttEvent(navident, orgEnhetId, gyldigTom);

        final OrgtilknytningState expectedOpprettet = lagStateObjekt(
            navident,
            orgEnhetId,
            sektor,
            false,
            gyldigFom,
            null,
            "S123456"
        );

        final OrgtilknytningState expectedDagligOppforingEndret = lagStateObjekt(
            navident,
            orgEnhetId,
            sektor,
            true,
            gyldigFom,
            null,
            "S123456"
        );

        final OrgtilknytningState expectedAvsluttet = lagStateObjekt(
            navident,
            orgEnhetId,
            sektor,
            true,
            gyldigFom,
            gyldigTom,
            "S123456"
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
        Sektor sektor = Sektor.NAV_STATLIG;
        LocalDate gyldigFom = LocalDate.of(2022, 1,  1);
        LocalDate gyldigTom = LocalDate.of(2022, 12, 31);
        LocalDate nyGyldigFom = LocalDate.of(2023, 1,  1);
        LocalDate nyGyldigTom = LocalDate.of(2023, 12, 31);

        final OrgtilknytningEvent opprettEvent = lagOpprettEvent(navident, orgEnhetId, sektor, gyldigFom);
        final OrgtilknytningEvent avsluttetEvent = lagAvsluttEvent(navident, orgEnhetId, gyldigTom);
        final OrgtilknytningEvent endretDatoEvent = lagDatoEndretEvent(navident, orgEnhetId, nyGyldigFom, nyGyldigTom);
        final OrgtilknytningEvent fjernetEvent = lagFjernetEvent(navident, orgEnhetId, nyGyldigFom, nyGyldigTom);

        final OrgtilknytningState expectedOpprettet = lagStateObjekt(
            navident,
            orgEnhetId,
            sektor,
            false,
            gyldigFom,
            null,
            "S123456"
        );

        final OrgtilknytningState expectedAvsluttet = lagStateObjekt(
            navident,
            orgEnhetId,
            sektor,
            false,
            gyldigFom,
            gyldigTom,
            "S123456"
        );

        final OrgtilknytningState expectedEndret = lagStateObjekt(
            navident,
            orgEnhetId,
            sektor,
            false,
            nyGyldigFom,
            nyGyldigTom,
            "S123456"
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
    @DisplayName("Send kun en Fjern orgtilknytning event - denne skal ikke behandles")
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

    @Test
    @DisplayName("OrgtilknytningTypeEndretEventV1 setter tilknytningtype")
    public void test_OrgtilknytningTypeEndretEventV1(){

        String navident = "A000001";
        String orgEnhetId = "'123bc";
        Sektor sektor = Sektor.NAV_KOMMUNAL;
        LocalDate gyldigFom = LocalDate.of(2022, 1,  1);

        final OrgtilknytningOpprettetEventV1 orgtilknytningOpprettetEventV1 = lagOpprettEvent(navident, orgEnhetId, sektor, LocalDate.of(2022, 1,  1));
        final OrgtilknytningTypeEndretEventV1 orgtilknytningTypeEndretEventV1 = new OrgtilknytningTypeEndretEventV1(
            orgtilknytningOpprettetEventV1.getFid(),
            new OrgtilknytningTypeEndretEventV1.OrgtilknytningTypeEndretEventData(Tilknytningtype.KONSULENT),
            (Event.Metadata) null
        );

        final OrgtilknytningState expectedEndret = lagStateObjekt(
            navident,
            orgEnhetId,
            sektor,
            false,
            gyldigFom,
            null,
            "S123456"
        ).toBuilder()
            .orgtilknytningType(orgtilknytningTypeEndretEventV1.getOrgtilknytningTypeEndretEventData().orgtilknytningType())
            .build();

        inputTopic.pipeInput(orgtilknytningOpprettetEventV1.getFid(), orgtilknytningOpprettetEventV1);
        inputTopic.pipeInput(orgtilknytningTypeEndretEventV1.getFid(), orgtilknytningTypeEndretEventV1);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .hasSize(2)
            .contains(KeyValue.pair(orgtilknytningTypeEndretEventV1.getFid(), expectedEndret));

    }

    @Test
    @DisplayName("OrgtilknytningTilknytningtypeEndretEventV1 setter tilknytningtype")
    public void test_OrgtilknytningTilknytningtypeEndretEventV1(){

        String navident = "A000001";
        String orgEnhetId = "'123bc";
        Sektor sektor = Sektor.EKSTERN;
        LocalDate gyldigFom = LocalDate.of(2022, 1,  1);

        final OrgtilknytningOpprettetEventV1 orgtilknytningOpprettetEventV1 = lagOpprettEvent(navident, orgEnhetId, sektor, LocalDate.of(2022, 1,  1));
        final OrgtilknytningTilknytningtypeEndretEventV1 orgtilknytningTilknytningtypeEndretEventV1 = new OrgtilknytningTilknytningtypeEndretEventV1(
            orgtilknytningOpprettetEventV1.getFid(),
            new OrgtilknytningTilknytningtypeEndretEventV1.OrgtilknytningTilknytningtypeEndretEventData(Tilknytningtype.KONSULENT),
            (Event.Metadata) null
        );

        final OrgtilknytningState expectedEndret = lagStateObjekt(
            navident,
            orgEnhetId,
            sektor,
            false,
            gyldigFom,
            null,
            "S123456"
        ).toBuilder()
            .orgtilknytningType(orgtilknytningTilknytningtypeEndretEventV1.getOrgtilknytningTilknytningtypeEndretEventData().orgtilknytningType())
            .build();

        inputTopic.pipeInput(orgtilknytningOpprettetEventV1.getFid(), orgtilknytningOpprettetEventV1);
        inputTopic.pipeInput(orgtilknytningTilknytningtypeEndretEventV1.getFid(), orgtilknytningTilknytningtypeEndretEventV1);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .hasSize(2)
            .contains(KeyValue.pair(orgtilknytningTilknytningtypeEndretEventV1.getFid(), expectedEndret));

    }

    @Test
    @DisplayName("Masseflytting - Avslutt gammel OT, Opprett ny OT, sett avsluttDato hvis tomDato satt, sett DOL, og til sist sett tilknytningtype")
    public void test_006() {
        String otFidEn = "OT-1234";
        String otFidTo = "OT-5678";
        String gammelOrgEnhetId = "'123ab";
        String nyOrgEnhetId = "'123bc";
        Sektor sektor = Sektor.NAV_STATLIG;
        LocalDate gammelGyldigFom = LocalDate.of(2022, 1, 1);
        LocalDate gyldigTom = LocalDate.now().plusYears(1);

        final OrgtilknytningEvent opprettEventGammelEn = lagOpprettEvent(otFidEn, "A000001", gammelOrgEnhetId, sektor, gammelGyldigFom, null);
        final OrgtilknytningEvent opprettEventGammelTo = lagOpprettEvent(otFidTo, "A000002", gammelOrgEnhetId, sektor, gammelGyldigFom, null);
        final OrgtilknytningEvent dolEventGammelEn = lagDagligOppfolgingEndretEvent(otFidEn, "A000001", gammelOrgEnhetId, gammelGyldigFom, null, true);
        final OrgtilknytningEvent dolEventGammelTo = lagDagligOppfolgingEndretEvent(otFidTo, "A000002", gammelOrgEnhetId, gammelGyldigFom, gyldigTom, true);
        final OrgtilknytningEvent avsluttEventGammelEn = lagAvsluttEvent(otFidEn, "A000001", gammelOrgEnhetId, LocalDate.now().minusDays(1));
        final OrgtilknytningEvent avsluttEventGammelTo = lagAvsluttEvent(otFidTo, "A000002", gammelOrgEnhetId, LocalDate.now().minusDays(1));

        final OrgtilknytningEvent opprettEventEn = lagMasseflyttetOpprettEvent("FID_1", "A000001", nyOrgEnhetId, sektor, LocalDate.now(), otFidEn);
        final OrgtilknytningEvent opprettEventTo = lagMasseflyttetOpprettEvent("FID_2", "A000002", nyOrgEnhetId, sektor, LocalDate.now(), otFidTo);
        final OrgtilknytningEvent avsluttetEventTo = lagAvsluttEvent("FID_2", "A000002", nyOrgEnhetId, gyldigTom, otFidTo);
        final OrgtilknytningEvent opprettDagligOppfolgingEventEn = lagDagligOppfolgingEndretEvent("FID_1", "A000001", nyOrgEnhetId, LocalDate.now(), null, true, otFidEn);
        final OrgtilknytningEvent opprettDagligOppfolgingEventTo = lagDagligOppfolgingEndretEvent("FID_2", "A000002", nyOrgEnhetId, LocalDate.now(), gyldigTom, true, otFidTo);
        final OrgtilknytningEvent endreTilnytningTypeEventEn = new OrgtilknytningTilknytningtypeEndretEventV1(
            "FID_1",
            new OrgtilknytningTilknytningtypeEndretEventV1.OrgtilknytningTilknytningtypeEndretEventData(Tilknytningtype.INGEN, otFidEn),
            (Event.Metadata) null
        );
        final OrgtilknytningEvent endreTilnytningTypeEventTo = new OrgtilknytningTilknytningtypeEndretEventV1(
            "FID_2",
            new OrgtilknytningTilknytningtypeEndretEventV1.OrgtilknytningTilknytningtypeEndretEventData(Tilknytningtype.INGEN, otFidTo),
            (Event.Metadata) null
        );

        final OrgtilknytningEvent endreTilnytningTypeEventToEtterMasseflytt = new OrgtilknytningTilknytningtypeEndretEventV1(
            "FID_2",
            new OrgtilknytningTilknytningtypeEndretEventV1.OrgtilknytningTilknytningtypeEndretEventData(Tilknytningtype.FAST),
            (Event.Metadata) null
        );

        final OrgtilknytningState expectedOpprettetGammelEn = lagStateObjekt(
            otFidEn,
            "A000001",
            gammelOrgEnhetId,
            sektor,
            false,
            gammelGyldigFom,
            null,
            null,
            null,
            "S123456"
        );

        final OrgtilknytningState expectedOpprettetGammelTo = lagStateObjekt(
            otFidTo,
            "A000002",
            gammelOrgEnhetId,
            sektor,
            false,
            gammelGyldigFom,
            null,
            null,
            null,
            "S123456"
        );

        final OrgtilknytningState expectedDolGammelEn = lagStateObjekt(
            otFidEn,
            "A000001",
            gammelOrgEnhetId,
            sektor,
            true,
            gammelGyldigFom,
            null,
            null,
            null,
            "S123456"
        );

        final OrgtilknytningState expectedDolGammelTo = lagStateObjekt(
            otFidTo,
            "A000002",
            gammelOrgEnhetId,
            sektor,
            true,
            gammelGyldigFom,
            gyldigTom,
            null,
            null,
            "S123456"
        );

        final OrgtilknytningState expectedAvsluttetGammelEn = lagStateObjekt(
            otFidEn,
            "A000001",
            gammelOrgEnhetId,
            sektor,
            true,
            gammelGyldigFom,
            LocalDate.now().minusDays(1),
            null,
            null,
            "S123456"
        );

        final OrgtilknytningState expectedAvsluttetGammelTo = lagStateObjekt(
            otFidTo,
            "A000002",
            gammelOrgEnhetId,
            sektor,
            true,
            gammelGyldigFom,
            LocalDate.now().minusDays(1),
            null,
            null,
            "S123456"
        );

        final OrgtilknytningState expectedOpprettetEn = lagStateObjekt(
            "FID_1",
            "A000001",
            nyOrgEnhetId,
            sektor,
            false,
            LocalDate.now(),
            null,
            null,
            otFidEn,
            "S123456"
        );

        final OrgtilknytningState expectedOpprettetTo = lagStateObjekt(
            "FID_2",
            "A000002",
            nyOrgEnhetId,
            sektor,
            false,
            LocalDate.now(),
            null,
            null,
            otFidTo,
            "S123456"
        );

        final OrgtilknytningState expectedAvsluttet = lagStateObjekt(
            "FID_2",
            "A000002",
            nyOrgEnhetId,
            sektor,
            false,
            LocalDate.now(),
            gyldigTom,
            null,
            otFidTo,
            "S123456"
        );

        final OrgtilknytningState expectedDagligOppfolgingEn = lagStateObjekt(
            "FID_1",
            "A000001",
            nyOrgEnhetId,
            sektor,
            true,
            LocalDate.now(),
            null,
            null,
            otFidEn,
            "S123456"
        );

        final OrgtilknytningState expectedDagligOppfolgingTo = lagStateObjekt(
            "FID_2",
            "A000002",
            nyOrgEnhetId,
            sektor,
            true,
            LocalDate.now(),
            gyldigTom,
            null,
            otFidTo,
            "S123456"
        );

        final OrgtilknytningState expectedTilknytningEndretEn = lagStateObjekt(
            "FID_1",
            "A000001",
            nyOrgEnhetId,
            sektor,
            true,
            LocalDate.now(),
            null,
            Tilknytningtype.INGEN,
            otFidEn,
            "S123456"
        );

        final OrgtilknytningState expectedTilknytningEndretTo = lagStateObjekt(
            "FID_2",
            "A000002",
            nyOrgEnhetId,
            sektor,
            true,
            LocalDate.now(),
            gyldigTom,
            Tilknytningtype.INGEN,
            otFidTo,
            "S123456"
        );

        final OrgtilknytningState expectedTilknytningEndretToEtterMasseflytt = lagStateObjekt(
            "FID_2",
            "A000002",
            nyOrgEnhetId,
            sektor,
            true,
            LocalDate.now(),
            gyldigTom,
            Tilknytningtype.FAST,
            null,
            "S123456"
        );


        inputTopic.pipeInput(opprettEventGammelEn.getFid(), opprettEventGammelEn);
        inputTopic.pipeInput(opprettEventGammelTo.getFid(), opprettEventGammelTo);
        inputTopic.pipeInput(dolEventGammelEn.getFid(), dolEventGammelEn);
        inputTopic.pipeInput(dolEventGammelTo.getFid(), dolEventGammelTo);
        inputTopic.pipeInput(avsluttEventGammelEn.getFid(), avsluttEventGammelEn);
        inputTopic.pipeInput(avsluttEventGammelTo.getFid(), avsluttEventGammelTo);
        inputTopic.pipeInput(opprettEventEn.getFid(), opprettEventEn);
        inputTopic.pipeInput(opprettEventTo.getFid(), opprettEventTo);
        inputTopic.pipeInput(avsluttetEventTo.getFid(), avsluttetEventTo);
        inputTopic.pipeInput(opprettDagligOppfolgingEventEn.getFid(), opprettDagligOppfolgingEventEn);
        inputTopic.pipeInput(opprettDagligOppfolgingEventTo.getFid(), opprettDagligOppfolgingEventTo);
        inputTopic.pipeInput(endreTilnytningTypeEventEn.getFid(), endreTilnytningTypeEventEn);
        inputTopic.pipeInput(endreTilnytningTypeEventTo.getFid(), endreTilnytningTypeEventTo);
        inputTopic.pipeInput(endreTilnytningTypeEventTo.getFid(), endreTilnytningTypeEventToEtterMasseflytt);

        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(opprettEventGammelEn.getFid(), expectedOpprettetGammelEn),
                KeyValue.pair(opprettEventGammelTo.getFid(), expectedOpprettetGammelTo),
                KeyValue.pair(dolEventGammelEn.getFid(), expectedDolGammelEn),
                KeyValue.pair(dolEventGammelTo.getFid(), expectedDolGammelTo),
                KeyValue.pair(avsluttEventGammelEn.getFid(), expectedAvsluttetGammelEn),
                KeyValue.pair(avsluttEventGammelTo.getFid(), expectedAvsluttetGammelTo),
                KeyValue.pair(opprettEventEn.getFid(), expectedOpprettetEn),
                KeyValue.pair(opprettEventTo.getFid(), expectedOpprettetTo),
                KeyValue.pair(avsluttetEventTo.getFid(), expectedAvsluttet),
                KeyValue.pair(opprettDagligOppfolgingEventEn.getFid(), expectedDagligOppfolgingEn),
                KeyValue.pair(opprettDagligOppfolgingEventTo.getFid(), expectedDagligOppfolgingTo),
                KeyValue.pair(endreTilnytningTypeEventEn.getFid(), expectedTilknytningEndretEn),
                KeyValue.pair(endreTilnytningTypeEventTo.getFid(), expectedTilknytningEndretTo),
                KeyValue.pair(endreTilnytningTypeEventTo.getFid(), expectedTilknytningEndretToEtterMasseflytt)
            );
    }

    private OrgtilknytningOpprettetEventV1 lagMasseflyttetOpprettEvent(String fid, String navident, String orgEnhetId, Sektor sektor, LocalDate gyldigFom, String masseflyttetFraFid) {
        return lagOpprettEvent(fid, navident, orgEnhetId, sektor, gyldigFom, masseflyttetFraFid);
    }

    private OrgtilknytningOpprettetEventV1 lagOpprettEvent(String navident, String orgEnhetId, Sektor sektor, LocalDate gyldigFom) {
        return lagOpprettEvent("FID_1", navident, orgEnhetId, sektor, gyldigFom, null);
    }

    private OrgtilknytningOpprettetEventV1 lagOpprettEvent(String fid, String navident, String orgEnhetId, Sektor sektor, LocalDate gyldigFom, String masseflyttetFraFid) {
        return new OrgtilknytningOpprettetEventV1(
            fid,
            new OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData(
                navident,
                orgEnhetId,
                sektor,
                gyldigFom,
                masseflyttetFraFid
            ),
            lagMetadata(),
            dummyAdditionalAgressoData
        );
    }

    private Event.Metadata lagMetadata() {
        return new Event.Metadata(
            new InputMetadata(
                "test",
                "test",
                null,
                new Origination(
                    "S123456",
                    "test"
                )
            ),
            LocalDateTime.now()
        );
    }

    private OrgtilknytningAvsluttetEventV1 lagAvsluttEvent(String navident, String orgEnhetId, LocalDate gyldigTom) {
        return lagAvsluttEvent("FID_1", navident, orgEnhetId, gyldigTom, null);
    }

    private OrgtilknytningAvsluttetEventV1 lagAvsluttEvent(String fid, String navident, String orgEnhetId, LocalDate gyldigTom) {
        return lagAvsluttEvent(fid, navident, orgEnhetId, gyldigTom, null);
    }

    private OrgtilknytningAvsluttetEventV1 lagAvsluttEvent(String fid, String navident, String orgEnhetId, LocalDate gyldigTom, String masseflyttetFraFid) {
        return new OrgtilknytningAvsluttetEventV1(
            fid,
            masseflyttetFraFid == null ?
                new OrgtilknytningAvsluttetEventV1.OrgtilknytningAvsluttetEventData(
                    navident,
                    orgEnhetId,
                    gyldigTom
                )
                : new OrgtilknytningAvsluttetEventV1.OrgtilknytningAvsluttetEventData(
                    navident,
                    orgEnhetId,
                    gyldigTom,
                    masseflyttetFraFid
                )
            ,
            (Event.Metadata) null,
            dummyAdditionalAgressoData
        );
    }

    private OrgtilknytningDagligOppfolgingEndretEventV1 lagDagligOppfolgingEndretEvent(String navident, String orgEnhetId, LocalDate gyldigFom, LocalDate gyldigTom, boolean dagligOppfolging) {
        return lagDagligOppfolgingEndretEvent("FID_1", navident, orgEnhetId, gyldigFom, gyldigTom, dagligOppfolging, null);
    }

    private OrgtilknytningDagligOppfolgingEndretEventV1 lagDagligOppfolgingEndretEvent(String fid, String navident, String orgEnhetId, LocalDate gyldigFom, LocalDate gyldigTom, boolean dagligOppfolging) {
        return lagDagligOppfolgingEndretEvent(fid, navident, orgEnhetId, gyldigFom, gyldigTom, dagligOppfolging, null);
    }

    private OrgtilknytningDagligOppfolgingEndretEventV1 lagDagligOppfolgingEndretEvent(String fid, String navident, String orgEnhetId, LocalDate gyldigFom, LocalDate gyldigTom, boolean dagligOppfolging, String masseflyttetFraFid) {
        return new OrgtilknytningDagligOppfolgingEndretEventV1(
            fid,
            masseflyttetFraFid == null ?
                new OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData(
                    navident,
                    orgEnhetId,
                    gyldigFom,
                    gyldigTom,
                    dagligOppfolging
                )
                : new OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData(
                    navident,
                    orgEnhetId,
                    gyldigFom,
                    gyldigTom,
                    dagligOppfolging,
                    masseflyttetFraFid
                )
            ,
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
            lagMetadata(),
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
                gyldigTom
            ),
            lagMetadata(),
            dummyAdditionalAgressoData
        );
    }

    private static OrgtilknytningState lagStateObjekt(String navident, String orgEnhetId, Sektor sektor, boolean erDagligOppfolging, LocalDate gyldigFom, LocalDate gyldigTom) {

        return OrgtilknytningState
            .builder()
            .fid("FID_1")
            .navident(navident)
            .orgEnhetId(orgEnhetId)
            .sektor(sektor)
            .erDagligOppfolging(erDagligOppfolging)
            .gyldigFom(gyldigFom)
            .gyldigTom(gyldigTom)
            .orgtilknytningType(null)
            .masseflyttetFraFid(null)
            .sistEndretAv(null)
            .build();
    }

    private static OrgtilknytningState lagStateObjekt(String navident, String orgEnhetId, Sektor sektor, boolean erDagligOppfolging, LocalDate gyldigFom, LocalDate gyldigTom, String sistEndretAv) {

        return OrgtilknytningState
            .builder()
            .fid("FID_1")
            .navident(navident)
            .orgEnhetId(orgEnhetId)
            .sektor(sektor)
            .erDagligOppfolging(erDagligOppfolging)
            .gyldigFom(gyldigFom)
            .gyldigTom(gyldigTom)
            .orgtilknytningType(null)
            .masseflyttetFraFid(null)
            .sistEndretAv(sistEndretAv)
            .build();
    }

    private static OrgtilknytningState lagStateObjekt(
        String fid,
        String navident,
        String orgEnhetId,
        Sektor sektor,
        boolean erDagligOppfolging,
        LocalDate gyldigFom,
        LocalDate gyldigTom,
        Tilknytningtype tilknytningtype,
        String masseflyttetFraFid,
        String sistEndretAv
    ) {

        return OrgtilknytningState
            .builder()
            .fid(fid)
            .navident(navident)
            .orgEnhetId(orgEnhetId)
            .sektor(sektor)
            .erDagligOppfolging(erDagligOppfolging)
            .gyldigFom(gyldigFom)
            .gyldigTom(gyldigTom)
            .orgtilknytningType(tilknytningtype)
            .masseflyttetFraFid(masseflyttetFraFid)
            .sistEndretAv(sistEndretAv)
            .build();
    }

    final OrgtilknytningEvent.AdditionalAgressoData dummyAdditionalAgressoData =
        new OrgtilknytningEvent.AdditionalAgressoData(
            "dummy_hrresStillingRessursEnhet_String",
            "dummy_hrresStillingerPerioderFormatert_String"
        );
    
    private void setupKafkaTransformer(){
        KafkaConfig kafkaConfig = new KafkaConfig();
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final var objectSerde = kafkaConfig.objectSerde(jsonMapper);


        new KafkaStreamProcessor(
                "inputTopic",
                "outputTopic",
            jsonMapper,
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

    private static JsonMapper configureJsonMapper() {
        return new JsonMapper();
    }


}

