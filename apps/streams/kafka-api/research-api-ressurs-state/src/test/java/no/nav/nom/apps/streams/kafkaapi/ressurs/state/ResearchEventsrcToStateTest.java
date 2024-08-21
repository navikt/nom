package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v2.Orgtilknytning;
import no.nav.nom.apps.streams.kafkaapi.ressurs.state.v2.RessursState;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import org.apache.kafka.streams.KeyValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ResearchEventsrcToStateTest extends ResearchEventsrcToStateTestTopology {

    @Test
    @DisplayName("RessursOpprettetEventV1 oppretter RessursState i output topic")
    public void test11() {
        String navident = "A123000";
        String ressursFid = "RESSURS_FID_1";

        final RessursOpprettetEventV1 ressursEvent = ressursOpprettetEvent(ressursFid, navident);

        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursEvent);

        RessursState firstExpectedStateEvt = ressursStateFraOpprettetEvent(ressursEvent);

        List<KeyValue<String, RessursState>> expected = new ArrayList<>();
        expected.add(new KeyValue<>(ressursFid, firstExpectedStateEvt));

        var result = outputTopic.readKeyValuesToList();

        log.info("Expected result: {}", asFormattedOutputString(expected));
        log.info("Acctual result: {}", asFormattedOutputString(result));

        assertThat(result)
            .containsExactly(
                toArray(expected)
            );
    }


    @Test
    @DisplayName("Merger RessursSluttet event merges sammen inn i state")
    public void test21() {
        String navident = "A123000";
        String ressursFid = "RESSURS_FID_1";

        final RessursOpprettetEventV1 ressursOpprettetEvent = ressursOpprettetEvent(ressursFid, navident);
        final RessursSluttetEventV1 ressursSluttetEvent = ressursSluttetEvent(ressursFid, LocalDate.now().plusMonths(2));

        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursOpprettetEvent);
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursSluttetEvent);

        RessursState firstExpectedStateEvt = ressursStateFraOpprettetEvent(ressursOpprettetEvent);
        RessursState nextExpectedStateEvt = oppdaterRessursState(firstExpectedStateEvt, ressursSluttetEvent);

        List<KeyValue<String, RessursState>> expected = new ArrayList<>();
        expected.add(new KeyValue<>(ressursFid, firstExpectedStateEvt));
        expected.add(new KeyValue<>(ressursFid, nextExpectedStateEvt));

        var result = outputTopic.readKeyValuesToList();

        log.info("Expected result: {}", asFormattedOutputString(expected));
        log.info("Acctual result: {}", asFormattedOutputString(result));

        assertThat(result)
            .containsExactly(
                toArray(expected)
            );
    }

    @Test
    @DisplayName("Merger PersonopplysningerEndret event merges sammen inn i state")
    public void test31() {
        String navident = "A123000";
        String ressursFid = "RESSURS_FID_1";

        final RessursOpprettetEventV1 ressursOpprettetEvent = ressursOpprettetEvent(ressursFid, navident);
        final RessursPersonaliaOppdatertV1 ressursPersonaliaOppdatertEvent = ressursPersonaliaOppdatertEvent(ressursFid,
            new Visningsnavn(
                "fornavnEndret",
                Optional.empty(),
                "etternavnEndret"
            )
        );

        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursOpprettetEvent);
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursPersonaliaOppdatertEvent);

        RessursState firstExpectedStateEvt = ressursStateFraOpprettetEvent(ressursOpprettetEvent);
        RessursState nextExpectedStateEvt = oppdaterRessursState(firstExpectedStateEvt, ressursPersonaliaOppdatertEvent);

        List<KeyValue<String, RessursState>> expected = new ArrayList<>();
        expected.add(new KeyValue<>(ressursFid, firstExpectedStateEvt));
        expected.add(new KeyValue<>(ressursFid, nextExpectedStateEvt));

        var result = outputTopic.readKeyValuesToList();

        log.info("Expected result: {}", asFormattedOutputString(expected));
        log.info("Acctual result: {}", asFormattedOutputString(result));

        assertThat(result)
            .containsExactly(
                toArray(expected)
            );
    }

    @Test
    @DisplayName("RessursOpprettet og OrgtilknytningOpprettet oppretter først RessursState uten orgtilknytninger, deretter med")
    public void test41() {
        String navident = "A123000";
        String enhet = "a123bc";
        String ressursFid = "RESSURS_FID_1";
        String orgtilknytningFid = "OT_FID_1";

        final RessursOpprettetEventV1 ressursEvent = ressursOpprettetEvent(ressursFid, navident);
        final OrgtilknytningOpprettetEventV1 orgtilknytningEvent = orgtilknytningOpprettetEvent(orgtilknytningFid, navident, enhet);

        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursEvent);
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningEvent);

        RessursState firstExpectedStateEvt = ressursStateFraOpprettetEvent(ressursEvent);
        RessursState nextExpectedStateEvt = oppdaterRessursState(firstExpectedStateEvt, orgtilknytningEvent);

        List<KeyValue<String, RessursState>> expected = new ArrayList<>();
        expected.add(new KeyValue<>(ressursFid, firstExpectedStateEvt));
        expected.add(new KeyValue<>(ressursFid, nextExpectedStateEvt));

        var result = outputTopic.readKeyValuesToList();

        log.info("Expected result: {}", asFormattedOutputString(expected));
        log.info("Acctual result: {}", asFormattedOutputString(result));

        assertThat(result)
            .containsExactly(
                toArray(expected)
            );
    }

    @Test
    @DisplayName("OrgtilknytningOpprettet tilfeldigvis kommer før RessursOpprettet oppretter kun en RessursState med tilhørende orgtilknytning")
    public void test42() {
        String navident = "A123000";
        String enhet = "a123bc";
        String ressursFid = "RESSURS_FID_1";
        String orgtilknytningFid = "OT_FID_1";

        final RessursOpprettetEventV1 ressursEvent = ressursOpprettetEvent(ressursFid, navident);
        final OrgtilknytningOpprettetEventV1 orgtilknytningEvent = orgtilknytningOpprettetEvent(orgtilknytningFid, navident, enhet);

        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningEvent);
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursEvent);

        RessursState stateEvtUtenOrgtilknytninger = ressursStateFraOpprettetEvent(ressursEvent);
        RessursState stateEvtMedOrgtilknytninger = oppdaterRessursState(stateEvtUtenOrgtilknytninger, orgtilknytningEvent);

        List<KeyValue<String, RessursState>> expected = new ArrayList<>();
        expected.add(new KeyValue<>(ressursFid, stateEvtMedOrgtilknytninger));

        var result = outputTopic.readKeyValuesToList();

        log.info("Expected result: {}", asFormattedOutputString(expected));
        log.info("Acctual result: {}", asFormattedOutputString(result));

        assertThat(result)
            .containsExactly(
                toArray(expected)
            );
    }

    @Test
    @DisplayName("Opprett ressurs, legg til orgtilknytning, sett daglig oppfølging, endre ressurs, avslutt orgtilknytning og til slutt avslutt ressurs")
    public void test51() {
        String navident = "A123000";
        String enhet = "a123bc";
        String ressursFid = "RESSURS_FID_1";
        String orgtilknytningFid = "OT_FID_1";
        LocalDate sluttdato = LocalDate.now().plusMonths(2);

        final RessursOpprettetEventV1 ressursEvent = ressursOpprettetEvent(ressursFid, navident);
        final OrgtilknytningOpprettetEventV1 orgtilknytningOpprettetEvent = orgtilknytningOpprettetEvent(orgtilknytningFid, navident, enhet);
        final OrgtilknytningDagligOppfolgingEndretEventV1 dagligOppfolgingEvent = dagligOppfolgingEvent(orgtilknytningOpprettetEvent, true);
        final RessursPersonaliaOppdatertV1 ressursPersonaliaOppdatertEvent = ressursPersonaliaOppdatertEvent(ressursFid,
            new Visningsnavn(
                "fornavnEndret",
                Optional.empty(),
                "etternavnEndret"
            )
        );
        final OrgtilknytningAvsluttetEventV1 orgtilknytningAvsluttetEvent =  orgtilknytningAvsluttetEvent(orgtilknytningOpprettetEvent, sluttdato);
        final RessursSluttetEventV1 ressursSluttetEvent = ressursSluttetEvent(ressursFid, sluttdato);

        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursEvent);
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent);
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), dagligOppfolgingEvent);
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursPersonaliaOppdatertEvent);
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningAvsluttetEvent);
        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursSluttetEvent);

        RessursState expectedStateEvt_1 = ressursStateFraOpprettetEvent(ressursEvent);
        RessursState expectedStateEvt_2 = oppdaterRessursState(expectedStateEvt_1, orgtilknytningOpprettetEvent);
        RessursState expectedStateEvt_3 = oppdaterRessursState(expectedStateEvt_2, dagligOppfolgingEvent);
        RessursState expectedStateEvt_4 = oppdaterRessursState(expectedStateEvt_3, ressursPersonaliaOppdatertEvent);
        RessursState expectedStateEvt_5 = oppdaterRessursState(expectedStateEvt_4, orgtilknytningAvsluttetEvent);
        RessursState expectedStateEvt_6 = oppdaterRessursState(expectedStateEvt_5, ressursSluttetEvent);

        List<KeyValue<String, RessursState>> expected = new ArrayList<>();
        expected.add(new KeyValue<>(ressursFid, expectedStateEvt_1));
        expected.add(new KeyValue<>(ressursFid, expectedStateEvt_2));
        expected.add(new KeyValue<>(ressursFid, expectedStateEvt_3));
        expected.add(new KeyValue<>(ressursFid, expectedStateEvt_4));
        expected.add(new KeyValue<>(ressursFid, expectedStateEvt_5));
        expected.add(new KeyValue<>(ressursFid, expectedStateEvt_6));

        var result = outputTopic.readKeyValuesToList();

        log.info("Expected result: {}", asFormattedOutputString(expected));
        log.info("Acctual result: {}", asFormattedOutputString(result));

        assertThat(result)
            .containsExactly(
                toArray(expected)
            );
    }

    @Test
    @DisplayName("Opprett ressurs, legg til 3 orgtilknytninger, endre ett par og forvent at sorteringen av tilknytninger i ressursen er uendret")
    public void test61() {
        String ressursFid = "RESSURS_FID_1";
        String navident = "A123000";
        String enhet_1 = "a123bc";
        String enhet_2 = "b234bd";
        String enhet_3 = "c345de";
        String orgtilknytningFid_1 = "OT_FID_1";
        String orgtilknytningFid_2 = "OT_FID_2";
        String orgtilknytningFid_3 = "OT_FID_3";
        LocalDate sluttdato = LocalDate.now().plusMonths(2);

        final RessursOpprettetEventV1 ressursEvent = ressursOpprettetEvent(ressursFid, navident);
        final OrgtilknytningOpprettetEventV1 orgtilknytningOpprettetEvent_1 = orgtilknytningOpprettetEvent(orgtilknytningFid_1, navident, enhet_1);
        final OrgtilknytningOpprettetEventV1 orgtilknytningOpprettetEvent_2 = orgtilknytningOpprettetEvent(orgtilknytningFid_2, navident, enhet_2);
        final OrgtilknytningOpprettetEventV1 orgtilknytningOpprettetEvent_3 = orgtilknytningOpprettetEvent(orgtilknytningFid_3, navident, enhet_3);
        final OrgtilknytningDagligOppfolgingEndretEventV1 dagligOppfolgingEvent = dagligOppfolgingEvent(orgtilknytningOpprettetEvent_2, true);
        final OrgtilknytningAvsluttetEventV1 orgtilknytningAvsluttetEvent =  orgtilknytningAvsluttetEvent(orgtilknytningOpprettetEvent_1, sluttdato);

        ressursInputTopic.pipeInput(UUID.randomUUID().toString(), ressursEvent);
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent_1);
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent_2);
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningOpprettetEvent_3);
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), dagligOppfolgingEvent);
        orgtilknytningInputTopic.pipeInput(UUID.randomUUID().toString(), orgtilknytningAvsluttetEvent);

        RessursState expectedStateEvt_1 = ressursStateFraOpprettetEvent(ressursEvent);
        RessursState expectedStateEvt_2 = oppdaterRessursState(expectedStateEvt_1, orgtilknytningOpprettetEvent_1);
        RessursState expectedStateEvt_3 = oppdaterRessursState(expectedStateEvt_2, orgtilknytningOpprettetEvent_2);
        RessursState expectedStateEvt_4 = oppdaterRessursState(expectedStateEvt_3, orgtilknytningOpprettetEvent_3);
        RessursState expectedStateEvt_5 = oppdaterRessursState(expectedStateEvt_4, dagligOppfolgingEvent);
        RessursState expectedStateEvt_6 = oppdaterRessursState(expectedStateEvt_5, orgtilknytningAvsluttetEvent);

        List<KeyValue<String, RessursState>> expected = new ArrayList<>();
        expected.add(new KeyValue<>(ressursFid, expectedStateEvt_1));
        expected.add(new KeyValue<>(ressursFid, expectedStateEvt_2));
        expected.add(new KeyValue<>(ressursFid, expectedStateEvt_3));
        expected.add(new KeyValue<>(ressursFid, expectedStateEvt_4));
        expected.add(new KeyValue<>(ressursFid, expectedStateEvt_5));
        expected.add(new KeyValue<>(ressursFid, expectedStateEvt_6));

        var result = outputTopic.readKeyValuesToList();

        log.info("Expected result: {}", asFormattedOutputString(expected));
        log.info("Acctual result: {}", asFormattedOutputString(result));

        assertThat(result)
            .containsExactly(
                toArray(expected)
            );
    }

    RessursOpprettetEventV1 ressursOpprettetEvent(String fid, String navident) {
        final RessursOpprettetEventV1.RessursOpprettetEventData eventData = new RessursOpprettetEventV1.RessursOpprettetEventData(
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
            (Event.Metadata) null,
            additionalData
        );
    }

    private RessursSluttetEventV1 ressursSluttetEvent(String fid, LocalDate sluttdato) {
        final RessursSluttetEventV1.RessursSluttetEventData eventData = new RessursSluttetEventV1.RessursSluttetEventData(
            sluttdato
        );

        return new RessursSluttetEventV1(
            fid,
            eventData,
            (Event.Metadata) null
        );
    }

    private RessursPersonaliaOppdatertV1 ressursPersonaliaOppdatertEvent(String fid, Visningsnavn visningsnavn) {
        final RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData eventData = new RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData(
            visningsnavn
        );
        return new RessursPersonaliaOppdatertV1(
            fid,
            eventData,
            (Event.Metadata) null
        );
    }

    private OrgtilknytningOpprettetEventV1 orgtilknytningOpprettetEvent(String fid, String navident, String enhet) {
        final OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData eventData = new OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData(
            navident,
            enhet,
            LocalDate.now()
        );

        return new OrgtilknytningOpprettetEventV1(
            fid,
            eventData,
            (Event.Metadata) null,
            null
        );
    }

    private OrgtilknytningDagligOppfolgingEndretEventV1 dagligOppfolgingEvent(OrgtilknytningOpprettetEventV1 opprettEvent, boolean isDagligOppfolging) {
        return dagligOppfolgingEvent(opprettEvent, isDagligOppfolging, null);
    }

    private OrgtilknytningDagligOppfolgingEndretEventV1 dagligOppfolgingEvent(OrgtilknytningOpprettetEventV1 opprettEvent,
                                                                              boolean isDagligOppfolging,
                                                                              LocalDate gyldigTom) {

        final OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData opprettetData =
            (OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData)opprettEvent.getData();

        final OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData eventData =
            new OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData(
                opprettetData.navident(),
                opprettetData.orgEnhetId(),
                opprettetData.gyldigFom(),
                Optional.ofNullable(gyldigTom),
                isDagligOppfolging
            );

        return new OrgtilknytningDagligOppfolgingEndretEventV1(
            opprettEvent.getFid(),
            eventData,
            (Event.Metadata) null,
            null
        );
    }

    private OrgtilknytningAvsluttetEventV1 orgtilknytningAvsluttetEvent(OrgtilknytningOpprettetEventV1 opprettEvent, LocalDate gyldigTom) {
        final OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData opprettetData =
            (OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData)opprettEvent.getData();

        final OrgtilknytningAvsluttetEventV1.OrgtilknytningAvsluttetEventData eventData =
            new OrgtilknytningAvsluttetEventV1.OrgtilknytningAvsluttetEventData(
                opprettetData.navident(),
                opprettetData.orgEnhetId(),
                gyldigTom
            );

        return new OrgtilknytningAvsluttetEventV1(
            opprettEvent.getFid(),
            eventData,
            (Event.Metadata) null,
            null
        );
    }

    RessursState ressursStateFraOpprettetEvent(RessursOpprettetEventV1 evt) {
        var data = (RessursOpprettetEventV1.RessursOpprettetEventData)evt.getData();
        var additionalData = (RessursOpprettetEventV1.RessursOpprettetAdditionalData)evt.getAdditionalData();

        return new RessursState(
            data.personident(),
            additionalData.navident().orElse("MANGLENDE_NAVIDENT"),
            data.visningsnavn(),
            Optional.empty(),
            data.sektor(),
            Optional.empty(),
            additionalData.startdato(),
            Optional.empty()
        );
    }

    private RessursState oppdaterRessursState(RessursState ressursState, RessursPersonaliaOppdatertV1 evt) {
        Visningsnavn visningsnavn = ((RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData)evt.getData()).visningsnavn();
        return new RessursState(
            ressursState.personident(),
            ressursState.navident(),
            visningsnavn,
            ressursState.primaryTelefonnummer(),
            ressursState.sektor(),
            ressursState.orgtilknytninger(),
            ressursState.startdato(),
            ressursState.sluttdato()
        );
    }

    private RessursState oppdaterRessursState(RessursState ressursState, RessursSluttetEventV1 evt) {
        LocalDate sluttdato = ((RessursSluttetEventV1.RessursSluttetEventData)evt.getData()).sluttdato();
        return new RessursState(
            ressursState.personident(),
            ressursState.navident(),
            ressursState.visningsnavn(),
            ressursState.primaryTelefonnummer(),
            ressursState.sektor(),
            ressursState.orgtilknytninger(),
            ressursState.startdato(),
            Optional.of(sluttdato)
        );
    }

    private RessursState oppdaterRessursState(RessursState ressursState, OrgtilknytningOpprettetEventV1 evt) {
        var orgtilknytninger = ressursState.orgtilknytninger().orElse(new ArrayList<>());
        String fid = evt.getFid();
        OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData evtData = (OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData)evt.getData();

        assert orgtilknytninger.stream().noneMatch(ot -> ot.fid().equals(fid)) : "RessursState inneholder allerede FID angitt i ny OrgtilknytningOpprettetEventV1";

        var nyOrgtilknytningerList = new ArrayList<>(orgtilknytninger);
        Orgtilknytning ny = new Orgtilknytning(
            evt.getFid(),
            evtData.orgEnhetId(),
            evtData.gyldigFom(),
            Optional.empty(),
            false
        );
        nyOrgtilknytningerList.add(ny);
        return ressursStateByttUtOrgtilknytninger(ressursState, nyOrgtilknytningerList);
    }

    private RessursState oppdaterRessursState(RessursState ressursState, OrgtilknytningDagligOppfolgingEndretEventV1 evt) {
        ArrayList<Orgtilknytning> orgtilknytninger = new ArrayList<>(ressursState.orgtilknytninger().orElse(new ArrayList<>()));
        String fid = evt.getFid();

        OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData evtData =
            (OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData)evt.getData();

        assert orgtilknytninger.stream().anyMatch(ot -> ot.fid().equals(fid)) : "RessursState inneholder ikke FID angitt i ny OrgtilknytningDagligOppfolgingEndretEventV1";


        var nyOrgtilknytningerList = new ArrayList<>(orgtilknytninger);
        var gammel = nyOrgtilknytningerList.stream().filter(ot -> ot.fid().equals(fid)).findFirst().get();

        Orgtilknytning ny = new Orgtilknytning(
            gammel.fid(),
            gammel.orgenhetId(),
            gammel.gyldigFom(),
            evtData.gyldigTom(),
            evtData.dagligOppfolging()
        );
        nyOrgtilknytningerList.set(orgtilknytninger.indexOf(gammel), ny);
        return ressursStateByttUtOrgtilknytninger(ressursState, nyOrgtilknytningerList);
    }

    private RessursState oppdaterRessursState(RessursState ressursState, OrgtilknytningAvsluttetEventV1 evt) {
        ArrayList<Orgtilknytning> orgtilknytninger = new ArrayList<>(ressursState.orgtilknytninger().orElse(new ArrayList<>()));
        String fid = evt.getFid();

        OrgtilknytningAvsluttetEventV1.OrgtilknytningAvsluttetEventData evtData =
            (OrgtilknytningAvsluttetEventV1.OrgtilknytningAvsluttetEventData)evt.getData();

        assert orgtilknytninger.stream().anyMatch(ot -> ot.fid().equals(fid)) : "RessursState inneholder ikke FID angitt i ny OrgtilknytningDagligOppfolgingEndretEventV1";


        var nyOrgtilknytningerList = new ArrayList<>(orgtilknytninger);
        var gammel = nyOrgtilknytningerList.stream().filter(ot -> ot.fid().equals(fid)).findFirst().get();

        Orgtilknytning ny = new Orgtilknytning(
            gammel.fid(),
            gammel.orgenhetId(),
            gammel.gyldigFom(),
            Optional.of(evtData.gyldigTom()),
            gammel.dagligOppfolging()
        );
        nyOrgtilknytningerList.set(orgtilknytninger.indexOf(gammel), ny);
        return ressursStateByttUtOrgtilknytninger(ressursState, nyOrgtilknytningerList);
    }

    private RessursState ressursStateByttUtOrgtilknytninger(RessursState ressursState,List<Orgtilknytning> orgtilknytninger) {
        var copy = new ArrayList<>(orgtilknytninger);
        return new RessursState(
            ressursState.personident(),
            ressursState.navident(),
            ressursState.visningsnavn(),
            ressursState.primaryTelefonnummer(),
            ressursState.sektor(),
            Optional.ofNullable(copy.isEmpty() ? null : copy),
            ressursState.startdato(),
            ressursState.sluttdato()
        );
    }

    private String asFormattedOutputString(List<KeyValue<String, RessursState>> keyValues) {
        StringBuffer strBuf = new StringBuffer();
        keyValues.forEach( kv -> strBuf.append("\n  %s -> %s".formatted(kv.key, kv.value)));
        return strBuf.toString();
    }

    @SuppressWarnings("unchecked")
    KeyValue<String, RessursState>[] toArray(List<KeyValue<String, RessursState>> list) {
        return list.toArray(KeyValue[]::new);
    }

}
