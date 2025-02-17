package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
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
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", DD_3Y, DD_1Y_1D)
        );

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        String fid = assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(RESSURS_1.navident())
                .tilEnhet(ENHET_A.getId())
                .gyldigFom(DD_3Y)
                .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid)
            .gyldigFom(DD_3Y)
            .gyldigTom(DD_1Y_1D);

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
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", DD_3Y, DD_1Y_1D),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", DD_1Y, NO_END_DATE)
        );

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        String fid = assertOrgtilknytning(CMD.OPPRETTET)
                .forNavident(RESSURS_1.navident())
                .tilEnhet(ENHET_A.getId())
                .gyldigFom(DD_3Y)
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
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", DD_3Y, DD_1Y_1D),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", DD_1Y, NO_END_DATE)
        );

        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_3Y, null, true)
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
            dvhRessursStilling("A000001", "0DD0001", "Enhet D2", DD_3Y, NO_END_DATE)
        );

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        String fid_d1 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .gyldigFom(DD_3Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_d1)
            .gyldigFom(DD_3Y)
            .gyldigTom(DD_1Y_1D);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_d1)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .erDagligOppfolging(true);


        String fid_d2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D2.getId())
            .gyldigFom(DD_1Y)
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
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0DD0001", DD_3Y, DD_1Y_1D),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0DD0001", DD_1Y, DD_6M_1D),
            dvhRessursStilling("A000001", "0DD0001", "Enhet D2","0DD0001", DD_2Y, NO_END_DATE)
        );

        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_3Y, DD_1Y_1D, true),
            orgtilknytning("FID_B", A000001, ENHET_B.getId(), DD_1Y, null, true)
        ));

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_A")
            .gyldigFom(DD_3Y)
            .gyldigTom(DD_2Y_1D);

        String fid_d1 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .gyldigFom(DD_2Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_d1)
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_1Y_1D);

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_d1)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .erDagligOppfolging(true);

        String fid_a2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_2Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_a2)
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_6M_1D);

        String fid_d2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D2.getId())
            .gyldigFom(DD_1Y)
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

    @Test
    @DisplayName("Tre DvhStillinger, to med samme sektor, en uten, men de to med sektor har forskjellige tilknytningtyper - Ref: 6")
    void e2e_test_6() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_3Y, DD_1Y_1D),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0AA0001", DD_1Y, DD_6M_1D, Optional.of("Midlertidig karakter § 9a")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A","0AA0001", DD_6M, NO_END_DATE, Optional.of("Fast"))
        );

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        String fid_a1 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_3Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_a1)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_a1)
            .gyldigFom(DD_3Y)
            .gyldigTom(DD_1Y_1D);

        String fid_a2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_1Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_a2)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_a2)
            .gyldigFom(DD_1Y)
            .gyldigTom(DD_6M_1D);

        String fid_a3 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_6M)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_a3)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .erDagligOppfolging(true);

        assertOrgtilknytning(CMD.ENDRE_SEKTOR)
            .forFid(fid_a3)
            .forNavident(RESSURS_1.navident())
            .tilSektor(Sektor.NAV_STATLIG)
            .tilEnhet(ENHET_A.getId());

        assertOrgtilknytning(CMD.ENDRE_TILKNYTNINGTYPE)
            .forFid(fid_a3)
            .tilTilknytningtype(Optional.of(Tilknytningtype.FAST));

        assertEquals(1, result.size());
        assertEquals(10, result.get(RESSURS_1.navident()).endringer().size());
        assertEquals(0, result.get(RESSURS_1.navident()).errors().size());
    }

    @Test
    @DisplayName("To DvhStillinger, en matcher med agressoId mot to Enheter, en matcher ikke Orgenhet På Ressurs, ulike OTer, like sektor/tilknytning - Ref: 7")
    void e2e_test_7() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0DD0001", DD_3Y, DD_1Y_1D, Optional.of("Fast")),
            dvhRessursStilling("A000001", "0AA0001", "Enhet A", "0DD0001", DD_1Y, DD_6M_1D, Optional.of("Fast")),
            dvhRessursStilling("A000001", "0DD0001", "Enhet D2","0DD0001", DD_2Y, NO_END_DATE, Optional.of("Fast"))
        );

        when(orgtilknytningService.getByNavIdent(A000001)).thenReturn(List.of(
            orgtilknytning("FID_A", A000001, ENHET_A.getId(), DD_3Y, DD_1Y_1D, true, Sektor.NAV_STATLIG, Tilknytningtype.FAST),
            orgtilknytning("FID_B", A000001, ENHET_B.getId(), DD_1Y, null, true, Sektor.NAV_STATLIG, Tilknytningtype.MIDLERTIDIG)
        ));

        var sspForNavidentResult = strukturer.strukturerStilingPerioderPerNavident(dvhRessursStillinger);
        var result = prosserer.process(sspForNavidentResult);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid("FID_A")
            .gyldigFom(DD_3Y)
            .gyldigTom(DD_2Y_1D);

        String fid_d1 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .gyldigFom(DD_2Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_d1)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D1.getId())
            .erDagligOppfolging(true);

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_d1)
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_1Y_1D);

        String fid_a2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_A.getId())
            .gyldigFom(DD_2Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRET_DATO)
            .forFid(fid_a2)
            .gyldigFom(DD_2Y)
            .gyldigTom(DD_6M_1D);

        String fid_d2 = assertOrgtilknytning(CMD.OPPRETTET)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D2.getId())
            .gyldigFom(DD_1Y)
            .getFid();

        assertOrgtilknytning(CMD.ENDRE_DAGLIG_OPPFOLGING)
            .forFid(fid_d2)
            .forNavident(RESSURS_1.navident())
            .tilEnhet(ENHET_D2.getId())
            .erDagligOppfolging(true);

        assertOrgtilknytning(CMD.ENDRE_SEKTOR)
            .forFid(fid_d2)
            .forNavident(RESSURS_1.navident())
            .tilSektor(Sektor.NAV_STATLIG)
            .tilEnhet(ENHET_D2.getId());

        assertOrgtilknytning(CMD.ENDRE_TILKNYTNINGTYPE)
            .forFid(fid_d2)
            .tilTilknytningtype(Optional.of(Tilknytningtype.FAST));

        assertOrgtilknytning(CMD.FJERNET)
            .forFid("FID_B");

        assertEquals(1, result.size());
        assertEquals(11, result.get(RESSURS_1.navident()).endringer().size());
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
            new SettDOLPerioderPaaStrukturerteStillinger(orgtilknytningStateRepository)
        );
        prosserer = new NyeRessursStillingProcessor(
            orgtilknytningService,
            orgtilknytningCommandService,
            ressursStateRepository,
            nomMastered,
            u4DiffPaaMastrede
        );
        when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(RESSURS_1));

        when(orgenhetService.exactMatchByAgressoId(ENHET_A.getAgressoId())).thenReturn(List.of(ENHET_A));
        when(orgenhetService.exactMatchByAgressoId(ENHET_B.getAgressoId())).thenReturn(List.of(ENHET_B));
        when(orgenhetService.exactMatchByAgressoId(ENHET_C.getAgressoId())).thenReturn(List.of(ENHET_C));
        when(orgenhetService.exactMatchByAgressoId(ENHET_D1.getAgressoId())).thenReturn(List.of(ENHET_D1, ENHET_D2));

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn("FID_1", "FID_2", "FID_3", "FID_4", "FID_5");
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(RESSURS_1));
    }

    static final String A000001 = "A000001";
    final RessursState RESSURS_1 = ressurs(A000001);

    final LocalDate NO_END_DATE = LocalDate.of(2099, 12, 31);
    final LocalDate DD_10Y = LocalDate.now().minusYears(10).withDayOfMonth(1);   // 10 år siden
    final LocalDate DD_3Y = LocalDate.now().minusYears(3).withDayOfMonth(1);    // 3 år siden
    final LocalDate DD_3Y_1D = DD_3Y.minusDays(1);                              // 3 år siden
    final LocalDate DD_2Y = LocalDate.now().minusYears(2).withDayOfMonth(1);    // 2 år siden
    final LocalDate DD_2Y_1D = DD_2Y.minusDays(1);                              // Dagen før 2 år siden
    final LocalDate DD_1Y = LocalDate.now().minusYears(1).withDayOfMonth(1);    // 1 år siden
    final LocalDate DD_1Y_1D = DD_1Y.minusDays(1);                              // Dagen før 1 år siden
    final LocalDate DD_6M = LocalDate.now().minusMonths(6).withDayOfMonth(1); // 6 måneder siden
    final LocalDate DD_6M_1D = DD_6M.minusDays(1);                              // Dagen før 6 måneder siden

//    final LocalDate DD_5Y = LocalDate.now().minusYears(5).withDayOfMonth(1); // 5 år siden
//    final LocalDate DD_4Y = LocalDate.now().minusYears(4).withDayOfMonth(1); // 4 år siden
//    final LocalDate DD_3Y2M = LocalDate.now().minusYears(3).minusMonths(2).withDayOfMonth(1); // 3 år, 2 måneder siden
//    final LocalDate DD_3Y = LocalDate.now().minusYears(3).withDayOfMonth(1); // 3 år siden
//    final LocalDate DD_2Y = LocalDate.now().minusYears(2).withDayOfMonth(1); // 2 år siden
//    final LocalDate DD_2Y_1D = DD_2Y.minusDays(1);                           // Dagen før 1 år siden
//    final LocalDate DD_1Y = LocalDate.now().minusYears(1).withDayOfMonth(1); // 1 år siden
//    final LocalDate DD_1Y_1D = DD_1Y.minusDays(1);                           // Dagen før 1 år siden
//    final LocalDate DD_6M = LocalDate.now().minusMonths(6).withDayOfMonth(1); // 6 måneder siden
//    final LocalDate DD_6M_1D = DD_6M.minusDays(1);                            // Dagen før 6 måneder siden

    final LocalDate START_OM_CA_2_MND = LocalDate.now().plusMonths(3).withDayOfMonth(1);
    final LocalDate SLUTT_OM_CA_2_MND = START_OM_CA_2_MND.minusDays(1);

    IOrgenhet ENHET_A = new OrgEnhet(
        new OrgEnhetState(
            "FID_A", "a100xy", "Enhet A",
            DD_10Y, Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("0AA0001"), Optional.of("ORGENHET"), Optional.empty()
        )
    );
    IOrgenhet ENHET_B = new OrgEnhet(
        new OrgEnhetState(
            "FID_B", "b100xy", "Enhet B",
            DD_10Y, Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("0BB0001"), Optional.of("ORGENHET"), Optional.empty()
        )
    );
    IOrgenhet ENHET_C = new OrgEnhet(
        new OrgEnhetState(
            "FID_C", "c100xy", "Enhet C",
            DD_10Y, Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("0CC0001"), Optional.of("ORGENHET"), Optional.empty()
        )
    );

    IOrgenhet ENHET_D1= new OrgEnhet(
        new OrgEnhetState(
            "FID_D1", "d100xy", "Enhet D1",
            DD_10Y, Optional.of(DD_1Y_1D),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("0DD0001"), Optional.of("ORGENHET"), Optional.empty()
        )
    );

    IOrgenhet ENHET_D2 = new OrgEnhet(
        new OrgEnhetState(
            "FID_D2", "d200xy", "Enhet D2",
            DD_1Y, Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("0DD0001"), Optional.of("ORGENHET"), Optional.empty()
        )
    );
}