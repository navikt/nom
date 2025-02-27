package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser.v1.RessursOpprettetEvent;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.*;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningAvsluttetEventV1.OrgtilknytningAvsluttetEventData;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1.RessursOpprettetAdditionalData;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1.RessursOpprettetEventData;
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
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT));

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent(ORGTILKNYTNING_FID_2, "B456789"));


        KeyValue<String, RessursOpprettetEvent> ressursOpprettetEvent = outputTopic.readKeyValue();
        log.info("Output event: {}", ressursOpprettetEvent);

        assertThat(ressursOpprettetEvent.value.navident()).isEqualTo(NAVIDENT);
        assertThat(ressursOpprettetEvent.value.orgtilknytning().fid()).isEqualTo(ORGTILKNYTNING_FID_1);
    }

    @Test
    void bare_behandle_ressurs_opprettet_events() {
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
        
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningDagligOppfolgingEndretEvent(ORGTILKNYTNING_FID_1, NAVIDENT));

        if (!outputTopic.isEmpty()) {
            KeyValue<String, RessursOpprettetEvent> ressursOpprettetEvent = outputTopic.readKeyValue();
            log.info("Output event: {}", ressursOpprettetEvent);
            
            var avsluttetEventData = (OrgtilknytningAvsluttetEventData) orgtilknytningAvsluttetEvent.getData();
            //noinspection OptionalGetWithoutIsPresent
            assertThat(ressursOpprettetEvent.value.orgtilknytning().gyldigTom().get()).isEqualTo(avsluttetEventData.gyldigTom());
        }
    }
    

    RessursOpprettetEventV1 ressursOpprettetEvent(String fid, String navident) {
        final RessursOpprettetEventData eventData = new RessursOpprettetEventData(
                "010101123456",
                Sektor.NAV_STATLIG,
                new Visningsnavn(
                        "fornavn",
                        Optional.empty(),
                        "etternavn"
                ),
                Optional.empty(),
                List.of("2021"),
                "2021"
        );

        final RessursOpprettetAdditionalData additionalData = new RessursOpprettetAdditionalData(
                Optional.of(navident),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(LocalDate.now()),
                Optional.empty(),
                Optional.empty()
        );

        return new RessursOpprettetEventV1(
                fid,
                eventData,
                (Event.Metadata) null,
                additionalData
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

    OrgtilknytningDagligOppfolgingEndretEventV1 orgtilknytningDagligOppfolgingEndretEvent(String fid, String navident) {
        final var eventData = new OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData(
                navident,
                ORGENHET_ID,
                LocalDate.now(),
                Optional.of(LocalDate.now().plusYears(1)),
                true
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

}
