package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v1.Hendelser;
import no.nav.nom.contract.nom.EntityType;
import no.nav.nom.contract.nom.HendelserType;
import no.nav.nom.contract.nom.eventsource.*;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.test.TestRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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
        } else if (event instanceof OrgtilknytningEvent) {
            orgtilknytningInputTopic.pipeInput(new TestRecord<>(UUID.randomUUID().toString(), (OrgtilknytningEvent) event));
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
        String orgTilknytningFid = "ORG_TILKNYTNING_FID_1";

        Event.Metadata eventMetadata = new Event.Metadata(null, LocalDateTime.now());

        //arrange orgenhet events og hendelser
        final OrgEnhetEvent opprettOrgenhetEvent = HendelserTestUtils.getOrgEnhetOpprettetEvent(orgEnhetFid, orgEnhetId, eventMetadata);
        final OrgEnhetEvent endreOrgenhetEvent = HendelserTestUtils.getOrgEnhetKorrigertEvent(orgEnhetFid, orgEnhetId, eventMetadata);
        final OrgEnhetEvent avvikleOrgenhetEvent = HendelserTestUtils.getOrgEnhetAvvikletEvent(orgEnhetFid, eventMetadata);
        final Hendelser opprettOrgenhetHendelser = new Hendelser(orgEnhetId, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelserType.NY);
        final Hendelser endreOrgenhetHendelser = new Hendelser(orgEnhetId, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelserType.ENDRET);
        final Hendelser avvikleOrgenhetHendelserWithNullId = new Hendelser(null, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelserType.FJERNET);
        final Hendelser endreOrgenhetHendelserWithNulId = new Hendelser(null, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelserType.ENDRET);


        //arrange resurs events og hendelser
        final RessursEvent opprettRessursEvent = HendelserTestUtils.getRessursOpprettetEvent(resursFid, navident, eventMetadata);
        final RessursEvent ressursPersonaliaOppdatertEvent = HendelserTestUtils.getRessursPersonaliaOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursPersonidentOppdatertEvent = HendelserTestUtils.getRessursPersonidentOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursSektorOppdatertEvent = HendelserTestUtils.getRessursSektorOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursStartDatoOppdatertEvent = HendelserTestUtils.getRessursStartDatoOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursSluttetEvent = HendelserTestUtils.getRessursSluttetEvent(resursFid, eventMetadata);
        final RessursEvent ressursFjernetEvent = HendelserTestUtils.getRessursTekniskSlettetEvent(resursFid, eventMetadata);

        final Hendelser opprettResursHendelser = new Hendelser(navident, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelserType.NY);
        final Hendelser endretResursHendelserWithId = new Hendelser(navident, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelserType.ENDRET);
        final Hendelser endretResursHendelserWithNullId = new Hendelser(null, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelserType.ENDRET);
        final Hendelser slettResursHendelserWithNullId = new Hendelser(null, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelserType.FJERNET);

        //arrange organisering events
        final OrganiseringEvent opprettOrganiseringEvent = HendelserTestUtils.getOpprettetOrganiseringEvent(organiseringFid, orgEnhetId, eventMetadata);
        final OrganiseringEvent endreOrganiseringEvent = HendelserTestUtils.getEndretOrganiseringEvent(organiseringFid, orgEnhetId, eventMetadata);
        final OrganiseringEvent avsluttOrganiseringEvent = HendelserTestUtils.getAvsluttetOrganiseringEvent(organiseringFid, eventMetadata);

        // orgtilknytning events
        final OrgtilknytningEvent orgtilknytningOpprettetEvent = HendelserTestUtils.getOrgtilknytningOpprettetEvent(orgTilknytningFid, navident, eventMetadata);
        final OrgtilknytningEvent getOrgtilknytningFjernetEvent = HendelserTestUtils.getOrgtilknytningFjernetEvent(orgTilknytningFid, navident, eventMetadata);
        final OrgtilknytningEvent getOrgtilknytningAvsluttetEvent = HendelserTestUtils.getOrgtilknytningAvsluttetEvent(
            orgTilknytningFid,
            navident,
            eventMetadata
        );
        final OrgtilknytningEvent getOrgtilknytningDatoEndretEvent = HendelserTestUtils.getOrgtilknytningDatoEndretEvent(
            orgTilknytningFid,
            navident,
            eventMetadata
        );
        final OrgtilknytningEvent getOrgtilknytningDagligOppfolgingEndretEvent = HendelserTestUtils.getOrgtilknytningDagligOppfolgingEndretEvent(
            orgTilknytningFid,
            navident,
            eventMetadata
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
            Arguments.of("sluttet resurs event oppretter endret Hendelser", ressursSluttetEvent, endretResursHendelserWithNullId),
            Arguments.of("fjernet resurs event oppretter fjernet Hendelser", ressursFjernetEvent, slettResursHendelserWithNullId),
            // organisering events
            Arguments.of("opprett organisering event oppretter endret Hendelser  ", opprettOrganiseringEvent, endreOrgenhetHendelser),
            Arguments.of("endre organisering event oppretter endret Hendelser  ", endreOrganiseringEvent, endreOrgenhetHendelser),
            Arguments.of("avslutt organisering event oppretter endret Hendelser  ", avsluttOrganiseringEvent, endreOrgenhetHendelserWithNulId),
            // orgtilknytning events
            Arguments.of("opprett tilknyting event oppretter endret Hendelser  ", orgtilknytningOpprettetEvent, endretResursHendelserWithId),
            Arguments.of("avslutt tilknyting event oppretter endret Hendelser  ", getOrgtilknytningAvsluttetEvent, endretResursHendelserWithId),
            Arguments.of("fjernet tilknyting event oppretter endret Hendelser  ", getOrgtilknytningFjernetEvent, endretResursHendelserWithId),
            Arguments.of("datoendret tilknyting event oppretter endret Hendelser  ", getOrgtilknytningDatoEndretEvent, endretResursHendelserWithId),
            Arguments.of(
                "dagligoppfolgingendret tilknyting event oppretter endret Hendelser  ",
                getOrgtilknytningDagligOppfolgingEndretEvent,
                endretResursHendelserWithId
            )
        );
    }

    @DisplayName("Test multiple events in a stream")
    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("getMultipleEventsInStream")
    void test_multiple_events_Hendelser(String ignoredDescription, List<Event> events, List<Hendelser> expectedHendelser) {
        // arrange
        events.forEach(event -> {
            if (event instanceof OrgEnhetEvent) {
                orgEnhetInputTopic.pipeInput(new TestRecord<>(UUID.randomUUID().toString(), (OrgEnhetEvent) event));
            } else if (event instanceof RessursEvent) {
                resursInputTopic.pipeInput(new TestRecord<>(UUID.randomUUID().toString(), (RessursEvent) event));
            } else if (event instanceof OrganiseringEvent) {
                organiseringInputTopic.pipeInput(new TestRecord<>(UUID.randomUUID().toString(), (OrganiseringEvent) event));
            } else if (event instanceof OrgtilknytningEvent) {
                orgtilknytningInputTopic.pipeInput(new TestRecord<>(UUID.randomUUID().toString(), (OrgtilknytningEvent) event));
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
        String orgTilknytningFid = "ORG_TILKNYTNING_FID_1";

        Event.Metadata eventMetadata = new Event.Metadata(null, LocalDateTime.now());

        // orgenhet events and hendelser
        final OrgEnhetEvent opprettOrgenhetEvent = HendelserTestUtils.getOrgEnhetOpprettetEvent(orgEnhetFid, orgEnhetId, eventMetadata);
        final OrgEnhetEvent endreOrgenhetEvent = HendelserTestUtils.getOrgEnhetKorrigertEvent(orgEnhetFid, orgEnhetId, eventMetadata);
        final OrgEnhetEvent avvikleOrgenhetEvent = HendelserTestUtils.getOrgEnhetAvvikletEvent(orgEnhetFid, eventMetadata);
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
        final OrganiseringEvent opprettOrganiseringEvent = HendelserTestUtils.getOpprettetOrganiseringEvent(organiseringFid, orgEnhetId, eventMetadata);
        final OrganiseringEvent avsluttOrganiseringEvent = HendelserTestUtils.getAvsluttetOrganiseringEvent(organiseringFid, eventMetadata);

        // resurs events and hendelser
        final RessursEvent opprettRessursEvent = HendelserTestUtils.getRessursOpprettetEvent(resursFid, navident, eventMetadata);
        final RessursEvent ressursPersonaliaOppdatertEvent = HendelserTestUtils.getRessursPersonaliaOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursPersonidentOppdatertEvent = HendelserTestUtils.getRessursPersonidentOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursSektorOppdatertEvent = HendelserTestUtils.getRessursSektorOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursStartDatoOppdatertEvent = HendelserTestUtils.getRessursStartDatoOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursSluttetEvent = HendelserTestUtils.getRessursSluttetEvent(resursFid, eventMetadata);
        final RessursEvent ressursFjernetEvent = HendelserTestUtils.getRessursTekniskSlettetEvent(resursFid, eventMetadata);
        final Hendelser opprettResursHendelser = new Hendelser(navident, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelserType.NY);
        final Hendelser endretResursHendelserWithId = new Hendelser(navident, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelserType.ENDRET);
        final Hendelser endretResursHendelserWithNullId = new Hendelser(null, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelserType.ENDRET);
        final Hendelser slettResursHendelserWithNullId = new Hendelser(null, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelserType.FJERNET);

        // orgtilknytning events and hendelser
        final OrgtilknytningEvent orgtilknytningOpprettetEvent = HendelserTestUtils.getOrgtilknytningOpprettetEvent(orgTilknytningFid, navident, eventMetadata);
        final OrgtilknytningEvent getOrgtilknytningFjernetEvent = HendelserTestUtils.getOrgtilknytningFjernetEvent(orgTilknytningFid, navident, eventMetadata);
        final OrgtilknytningEvent getOrgtilknytningAvsluttetEvent = HendelserTestUtils.getOrgtilknytningAvsluttetEvent(
            orgTilknytningFid,
            navident,
            eventMetadata
        );
        final OrgtilknytningEvent getOrgtilknytningDatoEndretEvent = HendelserTestUtils.getOrgtilknytningDatoEndretEvent(
            orgTilknytningFid,
            navident,
            eventMetadata
        );
        final OrgtilknytningEvent getOrgtilknytningDagligOppfolgingEndretEvent = HendelserTestUtils.getOrgtilknytningDagligOppfolgingEndretEvent(
            orgTilknytningFid,
            navident,
            eventMetadata
        );

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
                "avslutt -> opprett resurs event oppretter endret Hendelser uten id",
                Arrays.asList(ressursSluttetEvent, opprettRessursEvent),
                Arrays.asList(endretResursHendelserWithNullId, opprettResursHendelser)
            ),
            Arguments.of(
                "fjernet -> opprett resurs event oppretter fjernet Hendelser uten id",
                Arrays.asList(ressursFjernetEvent, opprettRessursEvent),
                Arrays.asList(slettResursHendelserWithNullId, opprettResursHendelser)
            ),
            Arguments.of(
                "avslutt -> opprett -> endre resurs event oppretter endret Hendelser med id",
                Arrays.asList(ressursSluttetEvent, opprettRessursEvent, ressursPersonaliaOppdatertEvent),
                Arrays.asList(endretResursHendelserWithNullId, opprettResursHendelser, endretResursHendelserWithId)
            ),
            Arguments.of(
                "endre -> avslutt resurs event oppretter endret Hendelser uten id",
                Arrays.asList(ressursPersonaliaOppdatertEvent, ressursSluttetEvent),
                Arrays.asList(endretResursHendelserWithNullId, endretResursHendelserWithNullId)
            ),
            // orgtilknytning events
            Arguments.of(
                "opprett orgtilknytning -> dato endret -> oppfolging endret -> fjern orgtilknytning events oppretter Hendelser med id",
                Arrays.asList(
                    orgtilknytningOpprettetEvent, getOrgtilknytningDatoEndretEvent, getOrgtilknytningDagligOppfolgingEndretEvent, getOrgtilknytningFjernetEvent
                ),
                Arrays.asList(
                    endretResursHendelserWithId, endretResursHendelserWithId, endretResursHendelserWithId, endretResursHendelserWithId)
            ),
            Arguments.of(
                "avslutt orgtilknytning -> fjern orgtilknytning events oppretter Hendelser med id",
                Arrays.asList(
                    getOrgtilknytningAvsluttetEvent, getOrgtilknytningFjernetEvent
                ),
                Arrays.asList(
                    endretResursHendelserWithId, endretResursHendelserWithId)
            ),
            // mixed event types
            Arguments.of(
                "opprett orgenhet -> opprett ressurs -> opprett organisering events oppretter Hendelser med id",
                Arrays.asList(
                    opprettOrgenhetEvent, opprettRessursEvent, opprettOrganiseringEvent
                ),
                Arrays.asList(
                    opprettOrgenhetHendelser, opprettResursHendelser, endreOrgenhetHendelser)
            )
        );
    }

}
