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

    @Test
    void produsere_ekstern_event_fra_event_source_event() {

        RessursOpprettetEventV1 ressursOpprettetEventV1 = ressursOpprettetEvent(RESSURS_FID, NAVIDENT);
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursOpprettetEventV1);

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT));
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true));

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_2, "B456789"));


        KeyValue<String, RessursOpprettetEvent> ressursOpprettetEvent = outputTopic.readKeyValue();
        log.info("Output event: {}", ressursOpprettetEvent);

        assertThat(ressursOpprettetEvent.value.navident()).isEqualTo(NAVIDENT);
        assertThat(ressursOpprettetEvent.value.orgtilknytning().fid()).isEqualTo(ORGTILKNYTNING_FID_1);
    }

    @Test
    void aggregere_ressurs_eventer_inntil_all_nodvendig_info_har_kommet_inn() {
        // RessursOpprettet-event uten startdato 
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursOpprettetEvent(RESSURS_FID, NAVIDENT, null));

        // Får så startdato i egen event
        RessursStartDatoOppdatertV1 ressursStartDatoOppdatert = ressursStartDatoOppdatertV1(RESSURS_FID, LocalDate.now());
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursStartDatoOppdatert);

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT));
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true));

        if (!outputTopic.isEmpty()) {
            KeyValue<String, RessursOpprettetEvent> ressursOpprettetEvent = outputTopic.readKeyValue();
            log.info("Output event: {}", ressursOpprettetEvent);

            var startDatoOppdatertEventData = (StartDatoOppdatertEventData) ressursStartDatoOppdatert.getData();
            assertThat(ressursOpprettetEvent.value.startdato()).isEqualTo(startDatoOppdatertEventData.startdato());
        }
    }

    @Test
    void bare_produsere_eksterne_events_for_ressurs_opprettet_events() {
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursSektorOppdatertEvent());

        assertThat(outputTopic.isEmpty()).as("Ingen output-meldinger er produsert").isTrue();
    }

    @Test
    void ikke_produsere_output_event_for_orgtilknytning_som_ikke_har_daglig_oppfolging() {
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursOpprettetEvent(RESSURS_FID, NAVIDENT));

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT));

        assertThat(outputTopic.isEmpty()).as("Ingen output-meldinger er produsert").isTrue();
    }

    @Test
    void aggregere_andre_orgtilknytning_events() {
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursOpprettetEvent(RESSURS_FID, NAVIDENT));

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_1, NAVIDENT));
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningSektorEndretEvent(ORGTILKNYTNING_FID_1));

        var orgtilknytningAvsluttetEvent = orgtilknytningAvsluttetEvent(ORGTILKNYTNING_FID_1);
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningAvsluttetEvent);

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT, true));

        if (!outputTopic.isEmpty()) {
            KeyValue<String, RessursOpprettetEvent> ressursOpprettetEvent = outputTopic.readKeyValue();
            log.info("Output event: {}", ressursOpprettetEvent);

            var avsluttetEventData = (OrgtilknytningAvsluttetEventData) orgtilknytningAvsluttetEvent.getData();
            //noinspection OptionalGetWithoutIsPresent
            assertThat(ressursOpprettetEvent.value.orgtilknytning().gyldigTom().get()).isEqualTo(avsluttetEventData.gyldigTom());
        }
    }

    @Test
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

        if (!outputTopic.isEmpty()) {
            assertThat(outputTopic.getQueueSize()).isEqualTo(1);
        }
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

    RessursSektorOppdatertV1 ressursSektorOppdatertEvent() {
        final var eventData = new RessursSektorOppdatertV1.SektorOppdatertEventData(
                Sektor.NAV_STATLIG
        );

        return new RessursSektorOppdatertV1(
                UUID.randomUUID().toString(),
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
