package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser.v1.RessursOpprettetEvent;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningAvsluttetEventV1.OrgtilknytningAvsluttetEventData;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDatoEndretEventV2;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningSektorEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningTilknytningtypeEndretEventV1;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursFysiskeBrukerstederOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursJobbtelefonOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1.RessursOpprettetAdditionalData;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1.RessursOpprettetEventData;
import no.nav.nom.contract.nom.eventsource.RessursPrimaryTelefonnummerOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSektorOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttdatoOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursStartDatoOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursStartDatoOppdatertV1.StartDatoOppdatertEventData;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import org.apache.kafka.streams.KeyValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class EventsourceToExternalEventsTest extends EventsourceToExternalEventsTestTopology {

    private final ObjectMapper prettyPrintMapper;

    private static final String RESSURS_FID = "Ressurs_FID_1";
    private static final String NAVIDENT = "A123456";
    private static final String ORGENHET_ID_1 = "ab123c";
    private static final String ORGTILKNYTNING_FID_1 = "OT_FID_1";
    private static final String ORGTILKNYTNING_FID_2 = "OT_FID_2";
    private static final String ORGTILKNYTNING_FID_3 = "OT_FID_3";
    private static final String ORGENHET_ID_2 = "de456f";
    private static final LocalDate NOW = LocalDate.now();

    public EventsourceToExternalEventsTest() {
        prettyPrintMapper = new ObjectMapper();
        prettyPrintMapper.enable(SerializationFeature.INDENT_OUTPUT);
        prettyPrintMapper.registerModule(new JavaTimeModule());
        prettyPrintMapper.registerModule(new Jdk8Module());
    }

    @Test
    @DisplayName("En output-melding produseres fra ressurs og OT event")
    void produsere_ekstern_event_fra_event_source_event() {

        RessursOpprettetEventV1 ressursOpprettetEventV1 = ressursOpprettetEvent(RESSURS_FID, NAVIDENT);
        sendToRessursTopic(ressursOpprettetEventV1);

        sendToOrgtilknytningTopic(orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT, ORGENHET_ID_1));
        sendToOrgtilknytningTopic(orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true));

        sendToOrgtilknytningTopic(orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_2, "B456789", ORGENHET_ID_1));

        assertThat(outputTopic.isEmpty()).as("Det er produsert en event").isFalse();
        KeyValue<String, RessursOpprettetEvent> ressursOpprettetEvent = outputTopic.readKeyValue();

        assertThat(ressursOpprettetEvent.value.getNavident()).isEqualTo(NAVIDENT);
        assertThat(ressursOpprettetEvent.value.getOrgtilknytning().fid()).isEqualTo(ORGTILKNYTNING_FID_1);
    }

    @Test
    @DisplayName("Aggregere ressurs-eventer til all nødvendig info har kommet inn")
    void aggregere_ressurs_eventer_inntil_all_nodvendig_info_har_kommet_inn() {
        // RessursOpprettet-event uten startdato
        sendToRessursTopic(ressursOpprettetEvent(RESSURS_FID, NAVIDENT, null));

        sendToRessursTopic(ressursSektorOppdatertEvent(RESSURS_FID, Sektor.NAV_KOMMUNAL));

        // Mottar så startdato i egen event
        RessursStartDatoOppdatertV1 ressursStartDatoOppdatert = ressursStartDatoOppdatertV1(RESSURS_FID, LocalDate.now());
        sendToRessursTopic(ressursStartDatoOppdatert);

        sendToOrgtilknytningTopic(orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT, ORGENHET_ID_1));
        sendToOrgtilknytningTopic(orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true));


        assertThat(outputTopic.isEmpty()).as("Det er produsert en event").isFalse();

        var startDatoOppdatertEventData = (StartDatoOppdatertEventData) ressursStartDatoOppdatert.getData();
        KeyValue<String, RessursOpprettetEvent> ressursOpprettetEvent = outputTopic.readKeyValue();
        assertThat(ressursOpprettetEvent.value.getStartdato()).isEqualTo(startDatoOppdatertEventData.startdato());
    }

    @Test
    @DisplayName("Bare produsere events for ressurs opprettet")
    void bare_produsere_eksterne_events_for_ressurs_opprettet_events() {
        sendToRessursTopic(ressursSektorOppdatertEvent(RESSURS_FID, Sektor.NAV_STATLIG));

        assertThat(outputTopic.isEmpty()).as("Ingen output-meldinger er produsert").isTrue();
    }

    @Test
    @DisplayName("Ikke produsere nye events ved endringer på ressurs etter output-melding er sendt")
    void ikke_produsere_nye_meldinger_ved_endringer_pa_ressurs_etter_output_melding_er_sendt() {
        sendToRessursTopic(ressursOpprettetEvent(RESSURS_FID, NAVIDENT, null));
        sendToRessursTopic(ressursStartDatoOppdatertV1(RESSURS_FID, LocalDate.now()));

        sendToOrgtilknytningTopic(orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT, ORGENHET_ID_1));
        sendToOrgtilknytningTopic(orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true));

        // Flere endringer for ressursen - skal ikke trigge nye output-meldinger
        sendToRessursTopic(ressursSektorOppdatertEvent(RESSURS_FID, Sektor.NAV_STATLIG));
        sendToRessursTopic(ressursStartDatoOppdatertV1(RESSURS_FID, LocalDate.now().plusWeeks(1)));
        sendToRessursTopic(ressursSektorOppdatertEvent(RESSURS_FID, Sektor.NAV_KOMMUNAL));

        assertThat(outputTopic.getQueueSize()).isEqualTo(1);
    }


    @Test
    @DisplayName("Flere ressurs opprettet events gir bare en output-hendelse")
    void flere_ressurs_opprettet_events_gir_bare_en_hendelse() {
        sendToRessursTopic(ressursOpprettetEvent(RESSURS_FID, NAVIDENT, null));
        sendToRessursTopic(ressursStartDatoOppdatertV1(RESSURS_FID, LocalDate.now().minusWeeks(1)));

        sendToOrgtilknytningTopic(orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT, ORGENHET_ID_1, NOW));
        sendToOrgtilknytningTopic(orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true));

        // Flere endringer for ressursen - skal ikke trigge nye output-meldinger
        sendToRessursTopic(ressursSektorOppdatertEvent(RESSURS_FID, Sektor.NAV_STATLIG));
        sendToRessursTopic(ressursStartDatoOppdatertV1(RESSURS_FID, LocalDate.now().minusDays(2)));
        sendToRessursTopic(ressursSektorOppdatertEvent(RESSURS_FID, Sektor.NAV_KOMMUNAL));

        // Ny ressurs opprettet-event ++
        sendToRessursTopic(ressursOpprettetEvent(RESSURS_FID, NAVIDENT, null));
        sendToRessursTopic(ressursStartDatoOppdatertV1(RESSURS_FID, LocalDate.now()));

        assertThat(outputTopic.getQueueSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("Ikke produsere event for orgtilknytning som ikke har DOL")
    void ikke_produsere_output_event_for_orgtilknytning_som_ikke_har_daglig_oppfolging() {
        sendToRessursTopic(ressursOpprettetEvent(RESSURS_FID, NAVIDENT));

        sendToOrgtilknytningTopic(orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT, ORGENHET_ID_1));

        assertThat(outputTopic.isEmpty()).as("Ingen output-meldinger er produsert").isTrue();
    }

    @Test
    @DisplayName("Aggregere orgtilknytning events frem til DOL-event mottas")
    void aggregere_andre_orgtilknytning_events() {
        sendToRessursTopic(ressursOpprettetEvent(RESSURS_FID, NAVIDENT));

        sendToOrgtilknytningTopic(orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT, ORGENHET_ID_1));
        sendToOrgtilknytningTopic(orgtilknytningSektorEndretEvent(ORGTILKNYTNING_FID_1));

        sendToOrgtilknytningTopic(orgtilknytningAvsluttetEvent(ORGTILKNYTNING_FID_1));

        var dolEvent = orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true);
        sendToOrgtilknytningTopic(dolEvent);

        assertThat(outputTopic.getQueueSize()).as("Det er produsert en event").isEqualTo(1);

        var dolEventData = (OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData) dolEvent.getData();
        KeyValue<String, RessursOpprettetEvent> ressursOpprettetEvent = outputTopic.readKeyValue();
        assertThat(ressursOpprettetEvent.value.getOrgtilknytning().gyldigTom()).isEqualTo(dolEventData.gyldigTom());
    }

    @Test
    @DisplayName("Endringer for orgtilknytning etter DOL-event er mottatt gir ikke nye events")
    void endringer_for_orgtilknytning_etter_dol_mottatt_gir_ikke_nye_hendelser() {
        sendToRessursTopic(ressursOpprettetEvent(RESSURS_FID, NAVIDENT));

        // Opprettet + DOL mottas først
        sendToOrgtilknytningTopic(orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT, ORGENHET_ID_1));
        sendToOrgtilknytningTopic(orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true));

        // Etter DOL er mottatt kommer flere endringer for OTen, inklusive nye DOL-meldinger
        sendToOrgtilknytningTopic(orgtilknytningTilknytningtypeEndretEvent());
        sendToOrgtilknytningTopic(orgtilknytningAvsluttetEvent(ORGTILKNYTNING_FID_1));
        sendToOrgtilknytningTopic(orgtilknytningDatoEndretEventV2(ORGTILKNYTNING_FID_1, LocalDate.now(), LocalDate.now().plusYears(2)));
        sendToOrgtilknytningTopic(orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_2, NAVIDENT, true));
        sendToOrgtilknytningTopic(orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_3, NAVIDENT, true));

        assertThat(outputTopic.getQueueSize())
            .as("Kun en RessursOpprettet-melding blir produsert")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Ved reell rekkefølge av events inkl. to OT opprettet produseres kun en output-hendelse")
    void reell_rekkefolge_med_to_ot_opprettet_gir_kun_en_hendelse() {
        // Rekkefølge av events er hentet fra reelt case i prod

        LocalDate startDato1 = LocalDate.of(2024, 1, 5);
        LocalDate sluttDato1 = LocalDate.of(2024, 5, 31);

        LocalDate startDato2 = LocalDate.of(2028, 8, 18);
        LocalDate sluttDato2 = LocalDate.of(2025, 12, 31);

        sendToRessursTopic(ressursOpprettetEvent(RESSURS_FID, NAVIDENT, null));
        sendToOrgtilknytningTopic(orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT, ORGENHET_ID_1, startDato1));
        sendToOrgtilknytningTopic(orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, ORGENHET_ID_1, true, startDato1, null));

        sendToRessursTopic(ressursStartDatoOppdatertV1(RESSURS_FID, startDato1));

        sendToRessursTopic(ressursFysiskeBrukerstederOppdatertEventV1(RESSURS_FID));
        sendToRessursTopic(ressursFysiskeBrukerstederOppdatertEventV1(RESSURS_FID));
        sendToRessursTopic(ressursFysiskeBrukerstederOppdatertEventV1(RESSURS_FID));

        sendToOrgtilknytningTopic(orgtilknytningAvsluttetEvent(ORGTILKNYTNING_FID_1, ORGENHET_ID_1, sluttDato1));
        sendToOrgtilknytningTopic(orgtilknytningDatoEndretEventV2(ORGTILKNYTNING_FID_1, startDato1, sluttDato1));
        sendToRessursTopic(ressursSluttetEventV1(RESSURS_FID, sluttDato1));

        sendToRessursTopic(ressursSluttetEventV1(RESSURS_FID, sluttDato2));
        sendToRessursTopic(ressursPrimaryTelefonnummerOppdatertEventV1(RESSURS_FID));
        sendToRessursTopic(ressursSektorOppdatertEvent(RESSURS_FID, Sektor.NAV_KOMMUNAL));
        sendToRessursTopic(ressursFysiskeBrukerstederOppdatertEventV1(RESSURS_FID));
        sendToRessursTopic(ressursSluttdatoOppdatertEventV1(RESSURS_FID, sluttDato2));
        sendToOrgtilknytningTopic(orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_2, NAVIDENT, ORGENHET_ID_2, startDato2));
        sendToOrgtilknytningTopic(orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_2, NAVIDENT, ORGENHET_ID_2, true, startDato2, sluttDato2));
        sendToOrgtilknytningTopic(orgtilknytningAvsluttetEvent(ORGTILKNYTNING_FID_2, ORGENHET_ID_2, sluttDato2));

        assertThat(outputTopic.getQueueSize()).isEqualTo(1);

        KeyValue<String, RessursOpprettetEvent> ressursOpprettetEvent = outputTopic.readKeyValue();
        log.info("Melding produsert: {}", prettyPrint(ressursOpprettetEvent));
    }

    @Test
    @DisplayName("Skal håndtere jobbtelefonOppdatertEvent med tomt data-felt")
    void skal_handtere_jobbtelefonOppdatertEvent_med_tomt_data_felt() {
        sendToRessursTopic(ressursOpprettetEvent(RESSURS_FID, NAVIDENT));

        RessursJobbtelefonOppdatertEventV1 jobbtelefonOppdatertEventV1 = new RessursJobbtelefonOppdatertEventV1(
            RESSURS_FID,
            null,
            new Event.Metadata(
                null,
                LocalDateTime.now()
            )
        );

        sendToRessursTopic(jobbtelefonOppdatertEventV1);


        // Opprettet + DOL mottas først
        sendToOrgtilknytningTopic(orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT, ORGENHET_ID_1));
        sendToOrgtilknytningTopic(orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true));

        assertThat(outputTopic.getQueueSize())
            .as("RessursOpprettet-melding har blitt produsert")
            .isEqualTo(1);
    }

    RessursOpprettetEventV1 ressursOpprettetEvent(String fid, String navident) {
        return ressursOpprettetEvent(fid, navident, LocalDate.now());
    }

    RessursOpprettetEventV1 ressursOpprettetEvent(String fid, String navident, LocalDate startDato) {
        final RessursOpprettetEventData eventData =
            RessursOpprettetEventData.builder()
                .personident("010101123456")
                .sektor(Sektor.NAV_STATLIG)
                .visningsnavn(new Visningsnavn(
                    "fornavn",
                    Optional.empty(),
                    "etternavn"
                ))
                .primaryTelefonnummer(null)
                .fysiskeBrukersteder(List.of("2021"))
                .primaryFysiskBrukersted("2021")
                .identType(IdentType.STANDARD)
                .build();

        final RessursOpprettetAdditionalData additionalData =
            RessursOpprettetAdditionalData.builder()
                .navident(navident)
                .kjoenn(null)
                .folkeregistrertNavn(null)
                .foedselsdato(null)
                .startdato(startDato)
                .orgEnhet(null)
                .ansettelsesType(null)
                .build();

        return new RessursOpprettetEventV1(
            fid,
            eventData,
            (Event.Metadata) null,
            additionalData
        );
    }

    RessursStartDatoOppdatertV1 ressursStartDatoOppdatertV1(String fid, LocalDate startDato) {
        final StartDatoOppdatertEventData eventData =
            new StartDatoOppdatertEventData(
                startDato
            );

        return new RessursStartDatoOppdatertV1(
            fid,
            eventData,
            (Event.Metadata) null
        );
    }

    OrgtilknytningOpprettetEventV1 orgtilknytningOpprettetEvent(String fid, String navident, String orgenhet) {
        return orgtilknytningOpprettetEvent(fid, navident, orgenhet, LocalDate.now());
    }

    OrgtilknytningOpprettetEventV1 orgtilknytningOpprettetEvent(String fid, String navident, String orgenhet, LocalDate fomDato) {
        final OrgtilknytningOpprettetEventData eventData =
            new OrgtilknytningOpprettetEventData(
                navident,
                orgenhet,
                Sektor.NAV_STATLIG,
                fomDato,
                null
            );

        return new OrgtilknytningOpprettetEventV1(
            fid,
            eventData,
            (Event.Metadata) null,
            null
        );
    }

    OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEvent(String fid, String navident, boolean dol) {
        return orgtilknytningDagligOppfolgingEndretEvent(fid, navident, ORGENHET_ID_1, dol, LocalDate.now(), LocalDate.now().plusYears(1));
    }

    OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEvent(
        String fid,
        String navident,
        String orgenhet,
        boolean dol,
        LocalDate fomDato,
        LocalDate tomDato
    ) {
        final var eventData = new OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData(
            navident,
            orgenhet,
            fomDato,
            tomDato,
            dol
        );

        return new OrgtilknytningDagligOppfolgingEndretEventV1(
            fid,
            eventData,
            (Event.Metadata) null,
            null
        );
    }

    RessursSektorOppdatertV1 ressursSektorOppdatertEvent(String fid, Sektor sektor) {
        final var eventData = new RessursSektorOppdatertV1.SektorOppdatertEventData(
            sektor
        );

        return new RessursSektorOppdatertV1(
            fid,
            eventData,
            (Event.Metadata) null
        );
    }

    OrgtilknytningSektorEndretEventV1 orgtilknytningSektorEndretEvent(String fid) {
        final var eventData = new OrgtilknytningSektorEndretEventV1.OrgtilknytningSektorEndretEventData(
            NAVIDENT,
            ORGENHET_ID_1,
            Sektor.NAV_KOMMUNAL
        );

        return new OrgtilknytningSektorEndretEventV1(
            fid,
            eventData,
            (Event.Metadata) null,
            null
        );
    }

    OrgtilknytningAvsluttetEventV1 orgtilknytningAvsluttetEvent(String fid) {
        return orgtilknytningAvsluttetEvent(fid, ORGENHET_ID_1, LocalDate.now());
    }

    OrgtilknytningAvsluttetEventV1 orgtilknytningAvsluttetEvent(String fid, String orgenhet, LocalDate tomDato) {
        final var eventData = new OrgtilknytningAvsluttetEventData(
            NAVIDENT,
            orgenhet,
            tomDato
        );

        return new OrgtilknytningAvsluttetEventV1(
            fid,
            eventData,
            (Event.Metadata) null,
            null
        );
    }

    OrgtilknytningTilknytningtypeEndretEventV1 orgtilknytningTilknytningtypeEndretEvent() {
        final var eventData = new OrgtilknytningTilknytningtypeEndretEventV1.OrgtilknytningTilknytningtypeEndretEventData(
            Tilknytningtype.ARBEIDSTRENING
        );

        return new OrgtilknytningTilknytningtypeEndretEventV1(
            ORGTILKNYTNING_FID_1,
            eventData,
            (Event.Metadata) null
        );
    }

    OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2(String fid, LocalDate gyldigFom, LocalDate gyldigTom) {
        final var eventData = new OrgtilknytningDatoEndretEventV2.OrgtilknytningDatoEndretEventData(
            gyldigFom,
            gyldigTom
        );

        return new OrgtilknytningDatoEndretEventV2(
            fid,
            eventData,
            (Event.Metadata) null,
            null
        );
    }

    RessursSluttetEventV1 ressursSluttetEventV1(String fid, LocalDate sluttdato) {
        final var eventData = new RessursSluttetEventV1.RessursSluttetEventData(
            sluttdato
        );

        return new RessursSluttetEventV1(
            fid,
            eventData,
            (Event.Metadata) null
        );
    }

    RessursSluttdatoOppdatertEventV1 ressursSluttdatoOppdatertEventV1(String fid, LocalDate sluttdato) {
        final var eventData = new RessursSluttdatoOppdatertEventV1.RessursSluttdatoOppdatertEventData(
            sluttdato
        );

        return new RessursSluttdatoOppdatertEventV1(
            fid,
            eventData,
            (Event.Metadata) null
        );
    }

    RessursFysiskeBrukerstederOppdatertEventV1 ressursFysiskeBrukerstederOppdatertEventV1(String fid) {
        final var eventData = new RessursFysiskeBrukerstederOppdatertEventV1.RessursFysiskeBrukerstederOppdatertEventData(
            List.of("1663"),
            "1663"
        );

        return new RessursFysiskeBrukerstederOppdatertEventV1(
            fid,
            eventData,
            (Event.Metadata) null
        );
    }

    RessursPrimaryTelefonnummerOppdatertEventV1 ressursPrimaryTelefonnummerOppdatertEventV1(String fid) {
        final var eventData = new RessursPrimaryTelefonnummerOppdatertEventV1.PrimaryTelefonnummerOppdatertEventData(
            "12345678"
        );

        return new RessursPrimaryTelefonnummerOppdatertEventV1(
            fid,
            eventData,
            (Event.Metadata) null
        );
    }

    private String prettyPrint(Object input) {
        try {
            return prettyPrintMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            return "An error occurred: " + e.getMessage();
        }
    }

    private void sendToRessursTopic(RessursEvent event) {
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), event);
    }

    private void sendToOrgtilknytningTopic(OrgtilknytningEvent event) {
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), event);
    }
}
