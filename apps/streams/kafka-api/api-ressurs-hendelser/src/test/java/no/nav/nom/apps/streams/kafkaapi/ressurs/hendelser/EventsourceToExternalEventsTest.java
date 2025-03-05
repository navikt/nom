package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser.v1.RessursOpprettetEvent;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.*;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningAvsluttetEventV1.OrgtilknytningAvsluttetEventData;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1.RessursOpprettetAdditionalData;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1.RessursOpprettetEventData;
import no.nav.nom.contract.nom.eventsource.RessursStartDatoOppdatertV1.StartDatoOppdatertEventData;
import org.apache.kafka.streams.KeyValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class EventsourceToExternalEventsTest extends EventsourceToExternalEventsTestTopology {

    private static final String RESSURS_FID = "Ressurs_FID_1";
    private static final String NAVIDENT = "A123456";
    private static final String ORGENHET_ID = "ab123c";
    private static final String ORGTILKNYTNING_FID_1 = "OT_FID_1";
    private static final String ORGTILKNYTNING_FID_2 = "OT_FID_2";
    private static final String ORGTILKNYTNING_FID_3 = "OT_FID_3";

    @Test
    @DisplayName("En output-melding produseres fra ressurs og OT event")
    void produsere_ekstern_event_fra_event_source_event() {

        RessursOpprettetEventV1 ressursOpprettetEventV1 = ressursOpprettetEvent(RESSURS_FID, NAVIDENT);
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursOpprettetEventV1);

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT));
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true));

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_2, "B456789"));


        assertThat(outputTopic.isEmpty()).as("Det er produsert en event").isFalse();
        KeyValue<String, RessursOpprettetEvent> ressursOpprettetEvent = outputTopic.readKeyValue();

        assertThat(ressursOpprettetEvent.value.navident()).isEqualTo(NAVIDENT);
        assertThat(ressursOpprettetEvent.value.orgtilknytning().fid()).isEqualTo(ORGTILKNYTNING_FID_1);
    }

    @Test
    @DisplayName("Aggregere ressurs-eventer til all nødvendig info har kommet inn")
    void aggregere_ressurs_eventer_inntil_all_nodvendig_info_har_kommet_inn() {
        // RessursOpprettet-event uten startdato 
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursOpprettetEvent(RESSURS_FID, NAVIDENT, null));

        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursSektorOppdatertEvent(RESSURS_FID, Sektor.NAV_KOMMUNAL));

        // Mottar så startdato i egen event
        RessursStartDatoOppdatertV1 ressursStartDatoOppdatert = ressursStartDatoOppdatertV1(RESSURS_FID, LocalDate.now());
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursStartDatoOppdatert);

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT));
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true));


        assertThat(outputTopic.isEmpty()).as("Det er produsert en event").isFalse();

        var startDatoOppdatertEventData = (StartDatoOppdatertEventData) ressursStartDatoOppdatert.getData();
        KeyValue<String, RessursOpprettetEvent> ressursOpprettetEvent = outputTopic.readKeyValue();
        assertThat(ressursOpprettetEvent.value.startdato()).isEqualTo(startDatoOppdatertEventData.startdato());
    }

    @Test
    @DisplayName("Bare produsere events for ressurs opprettet")
    void bare_produsere_eksterne_events_for_ressurs_opprettet_events() {
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursSektorOppdatertEvent(RESSURS_FID, Sektor.NAV_STATLIG));

        assertThat(outputTopic.isEmpty()).as("Ingen output-meldinger er produsert").isTrue();
    }

    @Test
    @DisplayName("Ikke produsere nye events ved endringer på ressurs etter output-melding er sendt")
    void ikke_produsere_nye_meldinger_ved_endringer_pa_ressurs_etter_output_melding_er_sendt() {
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursOpprettetEvent(RESSURS_FID, NAVIDENT, null));
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursStartDatoOppdatertV1(RESSURS_FID, LocalDate.now()));

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT));
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true));

        // Flere endringer for ressursen - skal ikke trigge nye output-meldinger
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursSektorOppdatertEvent(RESSURS_FID, Sektor.NAV_STATLIG));
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursStartDatoOppdatertV1(RESSURS_FID, LocalDate.now().plusWeeks(1)));
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursSektorOppdatertEvent(RESSURS_FID, Sektor.NAV_KOMMUNAL));

        assertThat(outputTopic.getQueueSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("Ikke produsere event for orgtilknytning som ikke har DOL")
    void ikke_produsere_output_event_for_orgtilknytning_som_ikke_har_daglig_oppfolging() {
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursOpprettetEvent(RESSURS_FID, NAVIDENT));

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT));

        assertThat(outputTopic.isEmpty()).as("Ingen output-meldinger er produsert").isTrue();
    }

    @Test
    @DisplayName("Ikke produsere flere events for orgtilknytning etter første event er sendt")
    void ikke_produsere_flere_output_events_for_dol_orgtilknytning_etter_output_event_er_sendt() {
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursOpprettetEvent(RESSURS_FID, NAVIDENT));

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT));
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true));

        // Etter DOL er mottatt kommer flere endringer for OTen, inklusive nye DOL-meldinger
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningTilknytningtypeEndretEvent());
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_2, NAVIDENT, true));
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_3, NAVIDENT, true));

        assertThat(outputTopic.getQueueSize())
                .as("Kun en RessursOpprettet-melding produsert")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Aggregere orgtilknytning events frem til DOL-event mottas")
    void aggregere_andre_orgtilknytning_events() {
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursOpprettetEvent(RESSURS_FID, NAVIDENT));

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT));
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningSektorEndretEvent(ORGTILKNYTNING_FID_1));

        var orgtilknytningAvsluttetEvent = orgtilknytningAvsluttetEvent(ORGTILKNYTNING_FID_1);
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningAvsluttetEvent);

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true));


        assertThat(outputTopic.isEmpty()).as("Det er produsert en event").isFalse();

        var avsluttetEventData = (OrgtilknytningAvsluttetEventData) orgtilknytningAvsluttetEvent.getData();
        KeyValue<String, RessursOpprettetEvent> ressursOpprettetEvent = outputTopic.readKeyValue();
        //noinspection OptionalGetWithoutIsPresent
        assertThat(ressursOpprettetEvent.value.orgtilknytning().gyldigTom().get()).isEqualTo(avsluttetEventData.gyldigTom());
    }

    @Test
    @DisplayName("Endringer for orgtilknytning etter DOL-event er mottatt gir ikke nye events")
    void endringer_for_orgtilknytning_etter_dol_mottatt_gir_ikke_nye_hendelser() {
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursOpprettetEvent(RESSURS_FID, NAVIDENT));

        // Opprettet + DOL mottas først
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT));
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true));

        // Etter DOL er mottatt kommer flere endringer for OTen
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningTilknytningtypeEndretEvent());
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningAvsluttetEvent(ORGTILKNYTNING_FID_1));
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(),
                orgtilknytningDatoEndretEventV2(LocalDate.now(), LocalDate.now().plusYears(2)));

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, false));

        assertThat(outputTopic.getQueueSize()).isEqualTo(1);
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
                        .primaryTelefonnummer(Optional.empty())
                        .fysiskeBrukersteder(List.of("2021"))
                        .primaryFysiskBrukersted("2021")
                        .identType(IdentType.STANDARD)
                        .build();

        final RessursOpprettetAdditionalData additionalData =
                RessursOpprettetAdditionalData.builder()
                        .navident(Optional.of(navident))
                        .kjoenn(Optional.empty())
                        .folkeregistrertNavn(Optional.empty())
                        .foedselsdato(Optional.empty())
                        .startdato(Optional.ofNullable(startDato))
                        .orgEnhet(Optional.empty())
                        .ansettelsesType(Optional.empty())
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

    OrgtilknytningOpprettetEventV1 orgtilknytningOpprettetEvent(String fid, String navident) {
        final OrgtilknytningOpprettetEventData eventData =
                new OrgtilknytningOpprettetEventData(
                        navident,
                        ORGENHET_ID,
                        Sektor.NAV_STATLIG,
                        LocalDate.now(),
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
        final var eventData = new OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData(
                navident,
                ORGENHET_ID,
                LocalDate.now(),
                Optional.of(LocalDate.now().plusYears(1)),
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
                ORGENHET_ID,
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
        final var eventData = new OrgtilknytningAvsluttetEventData(
                NAVIDENT,
                ORGENHET_ID,
                LocalDate.now().plusYears(1)
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
                Optional.of(Tilknytningtype.ARBEIDSTRENING)
        );

        return new OrgtilknytningTilknytningtypeEndretEventV1(
                ORGTILKNYTNING_FID_1,
                eventData,
                (Event.Metadata) null
        );
    }

    OrgtilknytningDatoEndretEventV2 orgtilknytningDatoEndretEventV2(LocalDate gyldigFom, LocalDate gyldigTom) {
        final var eventData = new OrgtilknytningDatoEndretEventV2.OrgtilknytningDatoEndretEventData(
                gyldigFom,
                Optional.of(gyldigTom)
        );

        return new OrgtilknytningDatoEndretEventV2(
                ORGTILKNYTNING_FID_1,
                eventData,
                (Event.Metadata) null,
                null
        );
    }

}
