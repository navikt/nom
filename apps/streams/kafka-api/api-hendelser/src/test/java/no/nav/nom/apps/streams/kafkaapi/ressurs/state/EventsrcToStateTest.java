package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v1.Hendelser;
import no.nav.nom.contract.nom.EntityType;
import no.nav.nom.contract.nom.HendelserType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.OrgEnhetAvvikletEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetKorrigertEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.contract.nom.eventsource.OrganiseringOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonidentOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSektorOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursStartDatoOppdatertV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.test.TestRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
public class EventsrcToStateTest extends EventsrcToStateTestTopology {

    @DisplayName("Test individual event in a stream")
    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("getIndividualEventInStream")
    void test_individual_event_Hendelser(String ignoredDescription, Event event, Hendelser expectedHendelser) {
        if (event instanceof OrgEnhetEvent) {
            orgEnhetInputTopic.pipeInput(new TestRecord<>(UUID.randomUUID().toString(), (OrgEnhetEvent) event));
        } else if (event instanceof RessursEvent) {
            resursInputTopic.pipeInput(new TestRecord<>(UUID.randomUUID().toString(), (RessursEvent) event));
        } else if (event instanceof OrganiseringEvent) {
            organiseringInputTopic.pipeInput(new TestRecord<>(UUID.randomUUID().toString(), (OrganiseringEvent) event));
        } else {
            throw new IllegalArgumentException("Unknown event type");
        }

        // assert
        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(event.getFid(), expectedHendelser)
            );
    }

    private static Stream<Arguments> getIndividualEventInStream() {
        String orgEnhetFid = "ORGENHET_FID_1";
        String orgEnhetId = "ORGENHET_ID_1";
        String navident = "A123000";
        String resursFid = "RESSURS_FID_1";
        String organiseringFid = "ORGANISERING_FID_1";
        Event.Metadata eventMetadata = new Event.Metadata(null, LocalDateTime.now());

        //arrange opprett orgEnhet event
        final OrgEnhetEvent opprettOrgenhetEvent = getOrgEnhetOpprettetEvent(orgEnhetFid, orgEnhetId, eventMetadata);
        final Hendelser opprettOrgenhetHendelser = new Hendelser(orgEnhetId, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelserType.NY);

        //arrange endre orgEnhet event
        final OrgEnhetEvent endreOrgenhetEvent = getOrgEnhetKorrigertEvent(orgEnhetFid, orgEnhetId, eventMetadata);
        final Hendelser endreOrgenhetHendelser = new Hendelser(orgEnhetId, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelserType.ENDRET);

        //arrange endre orgEnhet event
        final OrgEnhetEvent avvikleOrgenhetEvent = getOrgEnhetAvvikletEvent(orgEnhetFid, eventMetadata);
        final Hendelser avvikleOrgenhetHendelserWithNullId = new Hendelser(null, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelserType.FJERNET);

        //arrange opprett resurs event
        final RessursEvent opprettRessursEvent = getRessursOpprettetEvent(resursFid, navident, eventMetadata);
        final Hendelser opprettResursHendelser = new Hendelser(navident, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelserType.NY);

        //arrange endre resurs events
        final RessursEvent ressursPersonaliaOppdatertEvent = getRessursPersonaliaOppdatert(resursFid, navident, eventMetadata);
        final RessursEvent ressursPersonidentOppdatertEvent = getRessursPersonidentOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursSektorOppdatertEvent = getRessursSektorOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursStartDatoOppdatertEvent = getRessursStartDatoOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursSluttetEvent = getRessursSluttetEvent(resursFid, eventMetadata);
        final Hendelser endretResursHendelserWithNullId = new Hendelser(null, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelserType.ENDRET);
        final Hendelser slettResursHendelserWithNullId = new Hendelser(null, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelserType.FJERNET);

        //arrange organisering events
        final OrganiseringEvent opprettOrganiseringEvent = getOpprettetOrganiseringEvent(organiseringFid, orgEnhetId, eventMetadata);
        final OrganiseringEvent endreOrganiseringEvent = getEndretOrganiseringEvent(organiseringFid, orgEnhetId, eventMetadata);
        final Hendelser endreOrganiseringHendelser = new Hendelser(orgEnhetId, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelserType.ENDRET);

        final OrganiseringEvent avsluttOrganiseringEvent = getAvsluttetOrganiseringEvent(organiseringFid, eventMetadata);
        final Hendelser avsluttOrganiseringHendelserWithNulId = new Hendelser(
            null,
            eventMetadata.createdTimestamp(),
            EntityType.ORGENHET,
            HendelserType.ENDRET
        );

        return Stream.of(
            // orgeEnhet events
            Arguments.of("opprett orgEnhet event oppretter ny Hendelser  ", opprettOrgenhetEvent, opprettOrgenhetHendelser),
            Arguments.of("endre orgEnhet event oppretter endret Hendelser  ", endreOrgenhetEvent, endreOrgenhetHendelser),
            Arguments.of("avvikle orgEnhet event oppretter fjernet Hendelser  ", avvikleOrgenhetEvent, avvikleOrgenhetHendelserWithNullId),
            // ressurs events
            Arguments.of("opprett resurs event oppretter ny Hendelser", opprettRessursEvent, opprettResursHendelser),
            Arguments.of("personalia oppdatert resurs event oppretter endret Hendelser", ressursPersonaliaOppdatertEvent, endretResursHendelserWithNullId),
            Arguments.of("personident oppdatert resurs event oppretter endret Hendelser", ressursPersonidentOppdatertEvent, endretResursHendelserWithNullId),
            Arguments.of("sektor oppdatert resurs event oppretter endret Hendelser", ressursSektorOppdatertEvent, endretResursHendelserWithNullId),
            Arguments.of("startdato opdatert resurs event oppretter endret Hendelser", ressursStartDatoOppdatertEvent, endretResursHendelserWithNullId),
            Arguments.of("sluttet resurs event oppretter fjernet Hendelser", ressursSluttetEvent, slettResursHendelserWithNullId),
            // organisering events
            Arguments.of("opprett organisering event oppretter endret Hendelser  ", opprettOrganiseringEvent, endreOrganiseringHendelser),
            Arguments.of("endre organisering event oppretter endret Hendelser  ", endreOrganiseringEvent, endreOrganiseringHendelser),
            Arguments.of("avslutt organisering event oppretter endret Hendelser  ", avsluttOrganiseringEvent, avsluttOrganiseringHendelserWithNulId)
        );
    }

    @DisplayName("Test multiple events in a stream")
    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("getMultipleEventsInStream")
    void test_multiple_events_Hendelser(String ignoredDescription, List<Event> events, List<Hendelser> expectedHendelser) {
        // arrange
        events.stream().forEach(event -> {
            if (event instanceof OrgEnhetEvent) {
                orgEnhetInputTopic.pipeInput(new TestRecord<>(UUID.randomUUID().toString(), (OrgEnhetEvent) event));
            } else if (event instanceof RessursEvent) {
                resursInputTopic.pipeInput(new TestRecord<>(UUID.randomUUID().toString(), (RessursEvent) event));
            } else if (event instanceof OrganiseringEvent) {
                organiseringInputTopic.pipeInput(new TestRecord<>(UUID.randomUUID().toString(), (OrganiseringEvent) event));
            } else {
                throw new IllegalArgumentException("Unknown event type");
            }
        });

        // assert
        val result = outputTopic.readValuesToList();
        Assertions.assertThat(result.size()).isEqualTo(events.size());
        Assertions.assertThat(result).containsAll(expectedHendelser);
    }

    private static Stream<Arguments> getMultipleEventsInStream() {
        String orgEnhetFid = "ORGENHET_FID_1";
        String orgEnhetId = "ORGENHET_ID_1";
        String navident = "A123000";
        String resursFid = "RESSURS_FID_1";
        String organiseringFid = "ORGANISERING_FID_1";
        Event.Metadata eventMetadata = new Event.Metadata(null, LocalDateTime.now());

        // orgenhet events and hendelser
        final OrgEnhetEvent opprettOrgenhetEvent = getOrgEnhetOpprettetEvent(orgEnhetFid, orgEnhetId, eventMetadata);
        final OrgEnhetEvent endreOrgenhetEvent = getOrgEnhetKorrigertEvent(orgEnhetFid, orgEnhetId, eventMetadata);
        final OrgEnhetEvent avvikleOrgenhetEvent = getOrgEnhetAvvikletEvent(orgEnhetFid, eventMetadata);
        final Hendelser opprettOrgenhetHendelser = new Hendelser(orgEnhetId, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelserType.NY);
        final Hendelser endreOrgenhetHendelser = new Hendelser(orgEnhetId, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelserType.ENDRET);
        final Hendelser avvikleOrgenhetHendelserWithId = new Hendelser(
            orgEnhetId,
            eventMetadata.createdTimestamp(),
            EntityType.ORGENHET,
            HendelserType.FJERNET
        );
        final Hendelser avvikleOrgenhetHendelserWithNullId = new Hendelser(null, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelserType.FJERNET);

        // organisering events and hendelser
        final OrganiseringEvent opprettOrganiseringEvent = getOpprettetOrganiseringEvent(orgEnhetFid, orgEnhetId, eventMetadata);
        final OrganiseringEvent avsluttOrganiseringEvent = getAvsluttetOrganiseringEvent(orgEnhetFid, eventMetadata);

        // resurs events and hendelser
        final RessursEvent opprettRessursEvent = getRessursOpprettetEvent(resursFid, navident, eventMetadata);
        final RessursEvent ressursPersonaliaOppdatertEvent = getRessursPersonaliaOppdatert(resursFid, navident, eventMetadata);
        final RessursEvent ressursPersonidentOppdatertEvent = getRessursPersonidentOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursSektorOppdatertEvent = getRessursSektorOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursStartDatoOppdatertEvent = getRessursStartDatoOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursSluttetEvent = getRessursSluttetEvent(resursFid, eventMetadata);
        final Hendelser opprettResursHendelser = new Hendelser(navident, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelserType.NY);
        final Hendelser endretResursHendelserWithId = new Hendelser(navident, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelserType.ENDRET);
        final Hendelser endretResursHendelserWithNullId = new Hendelser(null, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelserType.ENDRET);
        final Hendelser slettResursHendelserWithNullId = new Hendelser(null, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelserType.FJERNET);


        // streams
        return Stream.of(
            // orgenhet events
            Arguments.of(
                "opprett -> endre -> avslutt orgenhet event oppretter fjernet Hendelser med id",
                Arrays.asList(opprettOrgenhetEvent, endreOrgenhetEvent, avvikleOrgenhetEvent),
                Arrays.asList(opprettOrgenhetHendelser, endreOrgenhetHendelser, avvikleOrgenhetHendelserWithId)
            ),
            Arguments.of(
                "avslutt -> opprett -> endre ->  orgenhet events oppretter fjernet Hendelser uten id",
                Arrays.asList(avvikleOrgenhetEvent, opprettOrgenhetEvent, endreOrgenhetEvent),
                Arrays.asList(avvikleOrgenhetHendelserWithNullId, opprettOrgenhetHendelser, endreOrgenhetHendelser)
            ),
            // organisering events
            Arguments.of(
                "opprett -> avslutt organisering events oppretter endret Hendelser med id",
                Arrays.asList(opprettOrganiseringEvent, avsluttOrganiseringEvent),
                Arrays.asList(endreOrgenhetHendelser, endreOrgenhetHendelser)
            ),
            Arguments.of(
                "avslutt -> opprett organisering events oppretter endret Hendelser med id",
                Arrays.asList(opprettOrganiseringEvent, avsluttOrganiseringEvent),
                Arrays.asList(endreOrgenhetHendelser, endreOrgenhetHendelser)
            ),
            // ressurs events
            Arguments.of(
                "opprett -> endre -> avslutt resurs event oppretter fjernet Hendelser med id",
                Arrays.asList(
                    opprettRessursEvent, ressursPersonaliaOppdatertEvent, ressursPersonidentOppdatertEvent,
                    ressursSektorOppdatertEvent, ressursStartDatoOppdatertEvent, ressursSluttetEvent
                ),
                Arrays.asList(
                    opprettResursHendelser, endretResursHendelserWithId, endretResursHendelserWithId,
                    endretResursHendelserWithId, endretResursHendelserWithId, endretResursHendelserWithId
                )
            ),
            Arguments.of(
                "avslutt -> opprett resurs event oppretter fjernet Hendelser uten id",
                Arrays.asList(ressursSluttetEvent, opprettRessursEvent),
                Arrays.asList(slettResursHendelserWithNullId, opprettResursHendelser)
            ),
            Arguments.of(
                "avslutt -> opprett -> endre resurs event oppretter endret Hendelser med id",
                Arrays.asList(ressursSluttetEvent, opprettRessursEvent, ressursPersonaliaOppdatertEvent),
                Arrays.asList(slettResursHendelserWithNullId, opprettResursHendelser, endretResursHendelserWithId)
            ),
            Arguments.of(
                "endre -> avslutt resurs event oppretter endret Hendelser uten id",
                Arrays.asList(ressursPersonaliaOppdatertEvent, ressursSluttetEvent),
                Arrays.asList(endretResursHendelserWithNullId, slettResursHendelserWithNullId)
            ),
            // mixed event types
            Arguments.of(
                "opprett orgenhet -> opprett ressurs -> opprett organisering events oppretter Hendelser med id",
                Arrays.asList(
                    opprettOrgenhetEvent, opprettRessursEvent, opprettOrganiseringEvent
                ),
                Arrays.asList(
                    opprettOrgenhetHendelser, opprettResursHendelser, endreOrgenhetHendelser))
        );
    }

    @Test
    @DisplayName("Test that AvsluttetOrganiseringEvent after OrganiseringOpprettetEvent produces Hendelser with correct Id")
    public void AvsluttetOrganiseringEvent_after_OrganiseringOpprettetEventV1_oppretter_ny_HendelserState() {
        String organiseringFid = "ORGANISERING_FID_1";
        String orgEnhetId = "ORGENHET_ID_1";

        //arrange opprett event
        Event.Metadata metadata = new Event.Metadata(null, LocalDateTime.now());
        final OrganiseringEvent opprettEvent = getOpprettetOrganiseringEvent(organiseringFid, orgEnhetId, metadata);
        final Hendelser expectedHendelser = new Hendelser(orgEnhetId, metadata.createdTimestamp(), EntityType.ORGENHET, HendelserType.ENDRET);

        //act
        String eventKey = UUID.randomUUID().toString();
        organiseringInputTopic.pipeInput(eventKey, opprettEvent);

        // assert that opprett event generates Hendelser with id
        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(organiseringFid, expectedHendelser)
            );

        //arrange slutt event
        final OrganiseringEvent avsluttEvent = getAvsluttetOrganiseringEvent(organiseringFid, metadata);
        String avsluttEventKey = UUID.randomUUID().toString();
        organiseringInputTopic.pipeInput(avsluttEventKey, avsluttEvent);

        // that avslutt event also generates Hendelser with id given that opprett event is already handled
        Assertions.assertThat(outputTopic.readKeyValuesToList())
            .containsExactly(
                KeyValue.pair(organiseringFid, expectedHendelser)
            );
    }

    private static OrgEnhetEvent getOrgEnhetOpprettetEvent(String fid, String id, Event.Metadata eventMetadata) {
        return new OrgEnhetOpprettetEventV1(
            fid,
            new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetEventData(
                id,
                "TestName",
                LocalDate.of(2022, 12, 1),
                Optional.of("orgEnhet_123"),
                Optional.of("ressurs_123")
            ),
            eventMetadata,
            new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalAgressoData(
                "AgressoId_123",
                "AgressoNavn",
                "AgressoOrgNivaa_1",
                Optional.of("AgressoOrgEnhetType"),
                "AgressoId_123-AgressoNavn-AgressoOrgNivaa_1"
            )
        );
    }

    private static OrgEnhetEvent getOrgEnhetKorrigertEvent(String fid, String id, Event.Metadata eventMetadata) {
        return new OrgEnhetKorrigertEventV1(
            fid,
            new OrgEnhetKorrigertEventV1.OrgEnhetKorrigertEventData(
                id,
                "TestName",
                LocalDate.of(2022, 12, 1),
                Optional.of(LocalDate.now().plusDays(10)),
                Optional.of("orgEnhet_123"),
                Optional.of("ressurs_123")
            ),
            eventMetadata,
            new OrgEnhetKorrigertEventV1.OrgEnhetKorrigertAdditionalAgressoData(
                "AgressoId_123",
                "AgressoNavn",
                "AgressoOrgNivaa_1",
                Optional.of("AgressoOrgEnhetType"),
                "AgressoId_123-AgressoNavn-AgressoOrgNivaa_1"
            )
        );
    }

    private static OrgEnhetEvent getOrgEnhetAvvikletEvent(String orgEnhetFid, Event.Metadata eventMetadata) {
        return new OrgEnhetAvvikletEventV1(
            orgEnhetFid,
            new OrgEnhetAvvikletEventV1.OrgEnhetAvvikletEventData(
                LocalDate.now().minusDays(2)
            ),
            eventMetadata
        );
    }

    private static OrganiseringEvent getOpprettetOrganiseringEvent(String fid, String id, Event.Metadata eventMetadata) {
        return new OrganiseringOpprettetEventV1(
            fid,
            new OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData(
                id,
                "navn",
                "idOver",
                "navnOver",
                LocalDate.of(2021, 1, 1),
                Optional.empty()
            ),
            eventMetadata
        );
    }

    private static OrganiseringEvent getEndretOrganiseringEvent(String fid, String id, Event.Metadata eventMetadata) {
        return new OrganiseringEndretEventV1(
            fid,
            new OrganiseringEndretEventV1.OrganiseringEndretEventData(
                id,
                "navn",
                "idOver",
                "navnOver",
                LocalDate.of(2021, 1, 1),
                Optional.empty()
            ),
            eventMetadata
        );
    }

    private static OrganiseringEvent getAvsluttetOrganiseringEvent(
        String fid,
        Event.Metadata eventMetadata
    ) {
        return new OrganiseringAvsluttetEventV1(
            fid,
            new OrganiseringAvsluttetEventV1.OrganiseringAvsluttetEventData(
                LocalDate.now().minusDays(2)
            ),
            eventMetadata
        );
    }

    static RessursEvent getRessursOpprettetEvent(String fid, String navident, Event.Metadata eventMetadata) {
        final RessursOpprettetEventV1.RessursOpprettetEventData eventData = new RessursOpprettetEventV1.RessursOpprettetEventData(
            "010101123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "fornavn",
                Optional.empty(),
                "etternavn"
            )
        );

        final RessursOpprettetEventV1.RessursOpprettetAdditionalData additionalData = new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
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
            eventMetadata,
            additionalData
        );
    }

    static RessursEvent getRessursPersonaliaOppdatert(String fid, String navident, Event.Metadata eventMetadata) {
        final RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData eventData = new RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData(
            new Visningsnavn(
                "fornavn",
                Optional.empty(),
                "etternavn"
            )
        );

        return new RessursPersonaliaOppdatertV1(
            fid,
            eventData,
            eventMetadata
        );
    }

    static RessursEvent getRessursPersonidentOppdatert(String fid, Event.Metadata eventMetadata) {
        final RessursPersonidentOppdatertV1.PersonidentOppdatertEventData eventData = new RessursPersonidentOppdatertV1.PersonidentOppdatertEventData(
            "010101123456"
        );

        return new RessursPersonidentOppdatertV1(
            fid,
            eventData,
            eventMetadata
        );
    }

    static RessursEvent getRessursSektorOppdatert(String fid, Event.Metadata eventMetadata) {
        final RessursSektorOppdatertV1.SektorOppdatertEventData eventData = new RessursSektorOppdatertV1.SektorOppdatertEventData(
            Sektor.NAV_STATLIG
        );

        return new RessursSektorOppdatertV1(
            fid,
            eventData,
            eventMetadata
        );
    }

    static RessursEvent getRessursStartDatoOppdatert(String fid, Event.Metadata eventMetadata) {
        final RessursStartDatoOppdatertV1.StartDatoOppdatertEventData eventData = new RessursStartDatoOppdatertV1.StartDatoOppdatertEventData(
            LocalDate.now().minusDays(50)
        );

        return new RessursStartDatoOppdatertV1(
            fid,
            eventData,
            eventMetadata
        );
    }

    static RessursEvent getRessursSluttetEvent(String fid, Event.Metadata eventMetadata) {
        final RessursSluttetEventV1.RessursSluttetEventData eventData = new RessursSluttetEventV1.RessursSluttetEventData(
            LocalDate.now().minusDays(50)
        );

        return new RessursSluttetEventV1(
            fid,
            eventData,
            eventMetadata
        );
    }
}
