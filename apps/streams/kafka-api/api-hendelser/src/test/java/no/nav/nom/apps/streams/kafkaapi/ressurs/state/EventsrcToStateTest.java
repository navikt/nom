package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v1.HendelserState;
import no.nav.nom.contract.nom.EntityType;
import no.nav.nom.contract.nom.HendelserType;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetOpprettetEventV1;
import org.apache.kafka.streams.KeyValue;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class EventsrcToStateTest extends EventsrcToStateTestTopology {

    @Test
    @DisplayName("OrgEnhetOpprettetEventV1 oppretter HendelserState i output topic")
    public void OrgEnhetOpprettetEventV1_oppretter_ny_HendelserState() {
        final OrgEnhetOpprettetEventV1.OrgEnhetOpprettetEventData eventData = new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetEventData(
            "DummyIntenrnId_1",
            "TestName",
            LocalDate.of(2022, 12, 1),
            Optional.of("orgEnhet_123"),
            Optional.of("ressurs_123")
        );

        final OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalAgressoData additionalData = new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalAgressoData(
            "AgressoId_123",
            "AgressoNavn",
            "AgressoOrgNivaa_1",
            Optional.of("AgressoOrgEnhetType"),
            "AgressoId_123-AgressoNavn-AgressoOrgNivaa_1"
        );

        Event.Metadata eventMetadata = new Event.Metadata(null, LocalDateTime.now());
        final OrgEnhetEvent event = new OrgEnhetOpprettetEventV1(
            UUID.randomUUID().toString(),
            eventData,
            eventMetadata,
            additionalData
        );

        final HendelserState expected = new HendelserState(
            eventData.id(),
            eventMetadata.createdTimestamp(),
            EntityType.ORGENHET,
            HendelserType.NY
        );

        orgEnhetInputTopic.pipeInput(UUID.randomUUID().toString(), event);

        // assert
        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(event.getFid(), expected)
            );
    }
}
