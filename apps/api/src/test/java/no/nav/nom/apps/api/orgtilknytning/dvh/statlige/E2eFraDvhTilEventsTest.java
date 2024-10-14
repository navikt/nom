package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode.DagligOppfolging;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.nav.nom.apps.api.orgtilknytning.dvh.statlige.NyeRessursStillingProcessor.LAGRE_ORGTILKNYTNINGER_I_NY_PROSESSERING_FOR_DVH_LAST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// REF: no/nav/nom/apps/api/orgtilknytning/dvh/dokumentasjon/TestReferanser_E2E_Fra_Dvh_til_events.md
@DisplayName("Tester prosessering av strukturerte StillingPerioder fra DVH")
class E2eFraDvhTilEventsTest extends TestBase {
    NyeRessursStillingStrukturerer strukturer;
    NyeRessursStillingProcessor prosserer;

    @Test
    @DisplayName("En DvhStilling, ingen eksisterende OT - Ref: 1")
    void e2e_test_1() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", _2022_01_01, _2024_01_31)
        );

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        String fid = assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(RESSURS_1.navident())
                .tilEnhet(ENHET_A.getId())
                .gyldigFom(_2022_01_01)
                .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid)
            .gyldigFom(_2022_01_01)
            .gyldigTom(_2024_01_31);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertEquals(1, result.size());
        assertEquals(3, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    @Test
    @DisplayName("To sammenhengende DvhStillinger, ingen eksisterende OT - Ref: 2")
    void e2e_test_2() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", _2022_01_01, _2023_01_31),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", _2023_02_01, NO_END_DATE)
        );

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        String fid = assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(RESSURS_1.navident())
                .tilEnhet(ENHET_A.getId())
                .gyldigFom(_2022_01_01)
                .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertEquals(1, result.size());
        assertEquals(2, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    @Test
    @DisplayName("To sammenhengende DvhStillinger, matcher eksisterende OT - Ref: 3")
    void e2e_test_3() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", _2022_01_01, _2023_01_31),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", _2023_02_01, NO_END_DATE)
        );

        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), _2022_01_01, null, true)
        ));

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        assertEquals(1, result.size());
        assertEquals(0, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    @Test
    @DisplayName("En DvhStilling, matcher med agressoId mot to Enheter - Ref: 4")
    void e2e_test_4() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0DD0001", "Enhet D2", _2022_01_01, NO_END_DATE)
        );

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        String fid_d1 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .gyldigFom(_2022_01_01)
            .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_d1)
            .gyldigFom(_2022_01_01)
            .gyldigTom(_2023_01_31);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_d1)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .erDagligOppfolging(true);


        String fid_d2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D2.getId())
            .gyldigFom(_2023_02_01)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_d2)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D2.getId())
            .erDagligOppfolging(true);

        assertEquals(1, result.size());
        assertEquals(5, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    @Test
    @DisplayName("To DvhStillinger, en matcher med agressoId mot to Enheter, en matcher ikke Orgenhet På Ressurs, ulike OTer - Ref: 5")
    void e2e_test_5() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0DD0001", _2022_01_01, _2023_01_31),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0DD0001", _2023_02_01, _2024_01_31),
            dvhRessursStilling("A000001", "0DD0001", "Enhet D2","0DD0001", _2022_05_01, NO_END_DATE)
        );

        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), _2022_01_01, _2023_01_31, true),
            orgtilknytning("FID_B", A000001, ENHET_B.getId(), _2023_02_01, null, true)
        ));

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_A")
            .gyldigFom(_2022_01_01)
            .gyldigTom(_2022_04_30);

        String fid_d1 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .gyldigFom(_2022_05_01)
            .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_d1)
            .gyldigFom(_2022_05_01)
            .gyldigTom(_2023_01_31);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_d1)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .erDagligOppfolging(true);

        String fid_a2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(_2022_05_01)
            .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_a2)
            .gyldigFom(_2022_05_01)
            .gyldigTom(_2024_01_31);

        String fid_d2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D2.getId())
            .gyldigFom(_2023_02_01)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_d2)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D2.getId())
            .erDagligOppfolging(true);

        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_B");

        assertEquals(1, result.size());
        assertEquals(9, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    // ----- Helpers

    @BeforeEach
    void fellesSetUp() {
        MockitoAnnotations.openMocks(this);
        strukturer = new NyeRessursStillingStrukturerer(
            ressursStateRepository,
            new GrupperOgSlaaSammenDvhStillinger(ressursStateRepository),
            new MatchStrukturerteStillingPerioderTilNOM(orgtilknytningStateRepository, orgenhetService),
            new SettDOLPerioderPaaStrukturerteStillinger(orgtilknytningStateRepository),
            nomMastered
        );
        prosserer = new NyeRessursStillingProcessor(
            orgtilknytningService,
            orgtilknytningCommandService,
            ressursStateRepository,
            unleash
        );
        when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(RESSURS_1));

        when(orgenhetService.exactMatchByAgressoId(ENHET_A.getAgressoId())).thenReturn(List.of(ENHET_A));
        when(orgenhetService.exactMatchByAgressoId(ENHET_B.getAgressoId())).thenReturn(List.of(ENHET_B));
        when(orgenhetService.exactMatchByAgressoId(ENHET_C.getAgressoId())).thenReturn(List.of(ENHET_C));
        when(orgenhetService.exactMatchByAgressoId(ENHET_D1.getAgressoId())).thenReturn(List.of(ENHET_D1, ENHET_D2));

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn("FID_1", "FID_2", "FID_3", "FID_4", "FID_5");
        when(unleash.isEnabled(eq(LAGRE_ORGTILKNYTNINGER_I_NY_PROSESSERING_FOR_DVH_LAST), anyBoolean())).thenReturn(true);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(RESSURS_1));
    }

    static final String A000001 = "A000001";
    final RessursState RESSURS_1 = ressurs(A000001);

    final LocalDate NO_END_DATE = LocalDate.of(2099, 12, 31);
    final LocalDate _2022_01_01 = LocalDate.of(2021, 1, 1);
    final LocalDate _2022_04_30 = LocalDate.of(2022, 4, 30);
    final LocalDate _2022_05_01 = LocalDate.of(2022, 5, 1);
    final LocalDate _2022_12_31 = LocalDate.of(2022, 12, 31);
    final LocalDate _2023_01_01 = LocalDate.of(2023, 1, 1);
    final LocalDate _2023_01_31 = LocalDate.of(2023, 1, 31);
    final LocalDate _2023_02_01 = LocalDate.of(2023, 2, 1);
    final LocalDate _2024_01_31 = LocalDate.of(2024, 1, 31);
    final LocalDate _2024_02_01 = LocalDate.of(2024, 2, 1);

    final LocalDate START_OM_CA_2_MND = LocalDate.now().plusMonths(3).withDayOfMonth(1);
    final LocalDate SLUTT_OM_CA_2_MND = START_OM_CA_2_MND.minusDays(1);

    IOrgenhet ENHET_A = new OrgEnhet(
        new OrgEnhetState(
            "FID_A", "a100xy", "Enhet A",
            LocalDate.of(2015, 1, 1), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("0AA0001"), Optional.of("ORGENHET"), Optional.empty()
        )
    );
    IOrgenhet ENHET_B = new OrgEnhet(
        new OrgEnhetState(
            "FID_B", "b100xy", "Enhet B",
            LocalDate.of(2015, 1, 1), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("0BB0001"), Optional.of("ORGENHET"), Optional.empty()
        )
    );
    IOrgenhet ENHET_C = new OrgEnhet(
        new OrgEnhetState(
            "FID_C", "c100xy", "Enhet C",
            LocalDate.of(2015, 1, 1), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("0CC0001"), Optional.of("ORGENHET"), Optional.empty()
        )
    );

    IOrgenhet ENHET_D1= new OrgEnhet(
        new OrgEnhetState(
            "FID_D1", "d100xy", "Enhet D1",
            LocalDate.of(2015, 1, 1), Optional.of(LocalDate.of(2023, 1, 31)),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("0DD0001"), Optional.of("ORGENHET"), Optional.empty()
        )
    );

    IOrgenhet ENHET_D2 = new OrgEnhet(
        new OrgEnhetState(
            "FID_D2", "d200xy", "Enhet D2",
            LocalDate.of(2023, 2, 1), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("0DD0001"), Optional.of("ORGENHET"), Optional.empty()
        )
    );
}