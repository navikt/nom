package no.nav.nom.apps.streams.kafkaapi.hendelser;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.streams.kafkaapi.hendelser.v1.Hendelse;
import no.nav.nom.contract.nom.EntityType;
import no.nav.nom.contract.nom.HendelseType;
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
public class EventsrcToHendelseTest extends EventsrcToHendelseTestTopology {

    @DisplayName("Test individual event in a stream")
    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("getIndividualEventInStream")
    void test_individual_event_Hendelser(String ignoredDescription, Event event, Hendelse expectedHendelse) {
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
                KeyValue.pair(event.getFid(), expectedHendelse)
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
        final Hendelse opprettOrgenhetHendelse = new Hendelse(orgEnhetId, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelseType.NY);
        final Hendelse endreOrgenhetHendelse = new Hendelse(orgEnhetId, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelseType.ENDRET);
        final Hendelse avvikleOrgenhetHendelseWithNullId = new Hendelse(null, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelseType.FJERNET);
        final Hendelse endreOrgenhetHendelseWithNulId = new Hendelse(null, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelseType.ENDRET);


        //arrange resurs events og hendelser
        final RessursEvent opprettRessursEvent = HendelserTestUtils.getRessursOpprettetEvent(resursFid, navident, eventMetadata);
        final RessursEvent ressursPersonaliaOppdatertEvent = HendelserTestUtils.getRessursPersonaliaOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursPersonidentOppdatertEvent = HendelserTestUtils.getRessursPersonidentOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursSektorOppdatertEvent = HendelserTestUtils.getRessursSektorOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursStartDatoOppdatertEvent = HendelserTestUtils.getRessursStartDatoOppdatert(resursFid, eventMetadata);
        final RessursEvent ressursSluttetEvent = HendelserTestUtils.getRessursSluttetEvent(resursFid, eventMetadata);
        final RessursEvent ressursFjernetEvent = HendelserTestUtils.getRessursTekniskSlettetEvent(resursFid, eventMetadata);

        final Hendelse opprettResursHendelse = new Hendelse(navident, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelseType.NY);
        final Hendelse endretResursHendelseWithId = new Hendelse(navident, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelseType.ENDRET);
        final Hendelse endretResursHendelseWithNullId = new Hendelse(null, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelseType.ENDRET);
        final Hendelse slettResursHendelseWithNullId = new Hendelse(null, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelseType.FJERNET);

        //arrange organisering events
        final OrganiseringEvent opprettOrganiseringEvent = HendelserTestUtils.getOpprettetOrganiseringEvent(organiseringFid, orgEnhetId, eventMetadata);
        final OrganiseringEvent endreOrganiseringEvent = HendelserTestUtils.getEndretOrganiseringEvent(organiseringFid, orgEnhetId, eventMetadata);
        final OrganiseringEvent avsluttOrganiseringEvent = HendelserTestUtils.getAvsluttetOrganiseringEvent(organiseringFid, eventMetadata);

        // orgtilknytning events
        final OrgtilknytningEvent orgtilknytningOpprettetEvent = HendelserTestUtils.getOrgtilknytningOpprettetEvent(orgTilknytningFid, navident, eventMetadata, null);
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
            Arguments.of("opprett orgEnhet event oppretter ny Hendelser  ", opprettOrgenhetEvent, opprettOrgenhetHendelse),
            Arguments.of("endre orgEnhet event oppretter endret Hendelser  ", endreOrgenhetEvent, endreOrgenhetHendelse),
            Arguments.of("avvikle orgEnhet event oppretter fjernet Hendelser  ", avvikleOrgenhetEvent, avvikleOrgenhetHendelseWithNullId),
            // ressurs events
            Arguments.of("opprett resurs event oppretter ny Hendelser", opprettRessursEvent, opprettResursHendelse),
            Arguments.of("personalia oppdatert resurs event oppretter endret Hendelser", ressursPersonaliaOppdatertEvent, endretResursHendelseWithNullId),
            Arguments.of("personident oppdatert resurs event oppretter endret Hendelser", ressursPersonidentOppdatertEvent, endretResursHendelseWithNullId),
            Arguments.of("sektor oppdatert resurs event oppretter endret Hendelser", ressursSektorOppdatertEvent, endretResursHendelseWithNullId),
            Arguments.of("startdato opdatert resurs event oppretter endret Hendelser", ressursStartDatoOppdatertEvent, endretResursHendelseWithNullId),
            Arguments.of("sluttet resurs event oppretter endret Hendelser", ressursSluttetEvent, endretResursHendelseWithNullId),
            Arguments.of("fjernet resurs event oppretter fjernet Hendelser", ressursFjernetEvent, slettResursHendelseWithNullId),
            // organisering events
            Arguments.of("opprett organisering event oppretter endret Hendelser  ", opprettOrganiseringEvent, endreOrgenhetHendelse),
            Arguments.of("endre organisering event oppretter endret Hendelser  ", endreOrganiseringEvent, endreOrgenhetHendelse),
            Arguments.of("avslutt organisering event oppretter endret Hendelser  ", avsluttOrganiseringEvent, endreOrgenhetHendelseWithNulId),
            // orgtilknytning events
            Arguments.of("opprett tilknyting event oppretter endret Hendelser  ", orgtilknytningOpprettetEvent, endretResursHendelseWithId),
            Arguments.of("avslutt tilknyting event oppretter endret Hendelser  ", getOrgtilknytningAvsluttetEvent, endretResursHendelseWithId),
            Arguments.of("fjernet tilknyting event oppretter endret Hendelser  ", getOrgtilknytningFjernetEvent, endretResursHendelseWithId),
            Arguments.of("datoendret tilknyting event oppretter endret Hendelser  ", getOrgtilknytningDatoEndretEvent, endretResursHendelseWithId),
            Arguments.of(
                "dagligoppfolgingendret tilknyting event oppretter endret Hendelser  ",
                getOrgtilknytningDagligOppfolgingEndretEvent,
                endretResursHendelseWithId
            )
        );
    }

    @DisplayName("Test multiple events in a stream")
    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("getMultipleEventsInStream")
    void test_multiple_events_Hendelser(String ignoredDescription, List<Event> events, List<Hendelse> expectedHendelse) {
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
        Assertions.assertThat(result).containsAll(expectedHendelse);
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
        final Hendelse opprettOrgenhetHendelse = new Hendelse(orgEnhetId, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelseType.NY);
        final Hendelse endreOrgenhetHendelse = new Hendelse(orgEnhetId, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelseType.ENDRET);
        final Hendelse avvikleOrgenhetHendelseWithId = new Hendelse(
            orgEnhetId,
            eventMetadata.createdTimestamp(),
            EntityType.ORGENHET,
            HendelseType.FJERNET
        );
        final Hendelse avvikleOrgenhetHendelseWithNullId = new Hendelse(null, eventMetadata.createdTimestamp(), EntityType.ORGENHET, HendelseType.FJERNET);

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
        final Hendelse opprettResursHendelse = new Hendelse(navident, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelseType.NY);
        final Hendelse endretResursHendelseWithId = new Hendelse(navident, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelseType.ENDRET);
        final Hendelse endretResursHendelseWithNullId = new Hendelse(null, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelseType.ENDRET);
        final Hendelse slettResursHendelseWithNullId = new Hendelse(null, eventMetadata.createdTimestamp(), EntityType.RESSURS, HendelseType.FJERNET);

        // orgtilknytning events and hendelser
        final OrgtilknytningEvent orgtilknytningOpprettetEvent = HendelserTestUtils.getOrgtilknytningOpprettetEvent(orgTilknytningFid, navident, eventMetadata, null);
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
                Arrays.asList(opprettOrgenhetHendelse, endreOrgenhetHendelse, avvikleOrgenhetHendelseWithId)
            ),
            Arguments.of(
                "avslutt -> opprett -> endre ->  orgenhet events oppretter fjernet Hendelser uten id",
                Arrays.asList(avvikleOrgenhetEvent, opprettOrgenhetEvent, endreOrgenhetEvent),
                Arrays.asList(avvikleOrgenhetHendelseWithNullId, opprettOrgenhetHendelse, endreOrgenhetHendelse)
            ),
            // organisering events
            Arguments.of(
                "opprett -> avslutt organisering events oppretter endret Hendelser med id",
                Arrays.asList(opprettOrganiseringEvent, avsluttOrganiseringEvent),
                Arrays.asList(endreOrgenhetHendelse, endreOrgenhetHendelse)
            ),
            Arguments.of(
                "avslutt -> opprett organisering events oppretter endret Hendelser med id",
                Arrays.asList(opprettOrganiseringEvent, avsluttOrganiseringEvent),
                Arrays.asList(endreOrgenhetHendelse, endreOrgenhetHendelse)
            ),
            // ressurs events
            Arguments.of(
                "opprett -> endre -> avslutt resurs event oppretter fjernet Hendelser med id",
                Arrays.asList(
                    opprettRessursEvent, ressursPersonaliaOppdatertEvent, ressursPersonidentOppdatertEvent,
                    ressursSektorOppdatertEvent, ressursStartDatoOppdatertEvent, ressursSluttetEvent
                ),
                Arrays.asList(
                    opprettResursHendelse, endretResursHendelseWithId, endretResursHendelseWithId,
                    endretResursHendelseWithId, endretResursHendelseWithId, endretResursHendelseWithId
                )
            ),
            Arguments.of(
                "avslutt -> opprett resurs event oppretter endret Hendelser uten id",
                Arrays.asList(ressursSluttetEvent, opprettRessursEvent),
                Arrays.asList(endretResursHendelseWithNullId, opprettResursHendelse)
            ),
            Arguments.of(
                "fjernet -> opprett resurs event oppretter fjernet Hendelser uten id",
                Arrays.asList(ressursFjernetEvent, opprettRessursEvent),
                Arrays.asList(slettResursHendelseWithNullId, opprettResursHendelse)
            ),
            Arguments.of(
                "avslutt -> opprett -> endre resurs event oppretter endret Hendelser med id",
                Arrays.asList(ressursSluttetEvent, opprettRessursEvent, ressursPersonaliaOppdatertEvent),
                Arrays.asList(endretResursHendelseWithNullId, opprettResursHendelse, endretResursHendelseWithId)
            ),
            Arguments.of(
                "endre -> avslutt resurs event oppretter endret Hendelser uten id",
                Arrays.asList(ressursPersonaliaOppdatertEvent, ressursSluttetEvent),
                Arrays.asList(endretResursHendelseWithNullId, endretResursHendelseWithNullId)
            ),
            // orgtilknytning events
            Arguments.of(
                "opprett orgtilknytning -> dato endret -> oppfolging endret -> fjern orgtilknytning events oppretter Hendelser med id",
                Arrays.asList(
                    orgtilknytningOpprettetEvent, getOrgtilknytningDatoEndretEvent, getOrgtilknytningDagligOppfolgingEndretEvent, getOrgtilknytningFjernetEvent
                ),
                Arrays.asList(
                    endretResursHendelseWithId, endretResursHendelseWithId, endretResursHendelseWithId, endretResursHendelseWithId)
            ),
            Arguments.of(
                "avslutt orgtilknytning -> fjern orgtilknytning events oppretter Hendelser med id",
                Arrays.asList(
                    getOrgtilknytningAvsluttetEvent, getOrgtilknytningFjernetEvent
                ),
                Arrays.asList(
                    endretResursHendelseWithId, endretResursHendelseWithId)
            ),
            // mixed event types
            Arguments.of(
                "opprett orgenhet -> opprett ressurs -> opprett organisering events oppretter Hendelser med id",
                Arrays.asList(
                    opprettOrgenhetEvent, opprettRessursEvent, opprettOrganiseringEvent
                ),
                Arrays.asList(
                    opprettOrgenhetHendelse, opprettResursHendelse, endreOrgenhetHendelse)
            )
        );
    }

}
