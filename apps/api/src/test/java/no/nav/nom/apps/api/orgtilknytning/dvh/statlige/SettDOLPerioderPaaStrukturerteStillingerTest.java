package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.CURRENT_PERIODE_KAN_IKKE_MATCHE_ORGENHET_PAA_RESSURS_FOR_DOL;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.DUPLIKATE_EKSISTERENDE_ORGTILKNYNINGER_I_HISTORISK_PERIODE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.DUPLIKATE_EKSISTERENDE_ORGTILKNYNINGER_MED_DOL_I_HISTORISK_PERIODE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.FUTURE_PERIODE_KAN_IKKE_SETTE_DOL;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.HISTORIC_PERIODE_KAN_IKKE_SETTE_DOL;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.HULL_I_STILLINGSPERIODE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.MULTI_MATCH_ORGENHET_PAA_RESSURS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

// REF: no/nav/nom/apps/api/orgtilknytning/dvh/dokumentasjon/TestReferanser_MatcheTilDagligOppfoelgingsLeder_DOL.md
@DisplayName("Tester for å sette DOL på strukturerte StillingPerioder fra DVH")
class SettDOLPerioderPaaStrukturerteStillingerTest extends TestBase {
    @Autowired
    SettDOLPerioderPaaStrukturerteStillinger CUT; // Class under test

    @Test
    @DisplayName("Match historisk SP - singel SP - ref: 1.i.a")
    void test_matchHistoriskSP_singleSP__1_i_a() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom)
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A.getId(), fom, tom, false),
            orgtilknytning("A000001", ENHET_B.getId(), fom, tom, true)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(1, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match historisk SP - match mot eksisterende OT - ref: 1.i.b")
    void test_matchHistoriskSP_motEksisterendeOT__1_i_b() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom),
            stillingPeriode(ENHET_B, fom, tom)
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A.getId(), fom, tom, true)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match historisk SP - match mot eksisterende OT - ref: 1.i.c")
    void test_matchHistoriskSP_motEksisterendeOT__1_i_c() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom),
            stillingPeriode(ENHET_B, fom, tom)
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A.getId(), fom, tom, true),
            orgtilknytning("A000001", ENHET_B.getId(), fom, tom, false),
            orgtilknytning("A000001", ENHET_C.getId(), fom, tom, false)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match historisk SP - match mot eksisterende OT - ref: 1.i.d")
    void test_matchHistoriskSP_motEksisterendeOT__1_i_d() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom),
            stillingPeriode(ENHET_B, fom, tom)
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A.getId(), fom, tom, true),
            orgtilknytning("A000001", ENHET_B.getId(), fom.minusMonths(2), fom.plusMonths(4).minusDays(1), false),
            orgtilknytning("A000001", ENHET_C.getId(), fom.plusMonths(2), tom.plusMonths(3), false)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match historisk SP - match mot eksisterende OT - ref: 1.i.e")
    void test_matchHistoriskSP_motEksisterendeOT__1_i_e() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom),
            stillingPeriode(ENHET_B, fom, tom)
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A.getId(), fom.minusMonths(2), tom.plusMonths(2), true),
            orgtilknytning("A000001", ENHET_B.getId(), fom, tom, false)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match historisk SP - match mot eksisterende OT - ref: 1.i.f")
    void test_matchHistoriskSP_motEksisterendeOT__1_i_f() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom),
            stillingPeriode(ENHET_B, fom, tom)
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A.getId(), fom.minusMonths(2), fom.plusMonths(2).minusDays(1), true),
            orgtilknytning("A000001", ENHET_A.getId(), fom.plusMonths(2), tom.plusMonths(2), true),
            orgtilknytning("A000001", ENHET_B.getId(), fom.minusMonths(4), fom.plusMonths(1).minusDays(1), false)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match historisk SP - match mot eksisterende OT - ref: 1.i.g")
    void test_matchHistoriskSP_motEksisterendeOT__1_i_g() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, "Enhet_X"),
            stillingPeriode(ENHET_B, fom, tom)
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A.getId(), fom, tom, false),
            orgtilknytning("A000001", ENHET_A.getId(), fom, tom, false),
            orgtilknytning("A000001", ENHET_B.getId(), fom, tom, true)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_B.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_A.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(DUPLIKATE_EKSISTERENDE_ORGTILKNYNINGER_I_HISTORISK_PERIODE));
    }

    @Test
    @DisplayName("Match historisk SP - match mot eksisterende OT - ref: 1.i.h")
    void test_matchHistoriskSP_motEksisterendeOT__1_i_h() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_A.getAgressoId()),
            stillingPeriode(ENHET_B, fom, tom, ENHET_A.getAgressoId())
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A.getId(), fom, tom, true),
            orgtilknytning("A000001", ENHET_B.getId(), fom, tom, true),
            orgtilknytning("A000001", ENHET_C.getId(), fom, tom, false)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(DUPLIKATE_EKSISTERENDE_ORGTILKNYNINGER_MED_DOL_I_HISTORISK_PERIODE));
    }

    @Test
    @DisplayName("Match historisk SP - ingen match mot eksisterende OT - match mot Orgenhet på ressurs (OPR) - ref: 1.ii.a")
    void test_matchHistoriskSP_ingenMatchMotEksisterendeOT_matchMotOPR__1_ii_a() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_A.getAgressoId()),
            stillingPeriode(ENHET_B, fom, tom, ENHET_A.getAgressoId())
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match historisk SP - ingen match mot eksisterende OT - match mot Orgenhet på ressurs (OPR) - ref: 1.ii.b")
    void test_matchHistoriskSP_ingenMatchMotEksisterendeOT_matchMotOPR__1_ii_b() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_B.getAgressoId()),
            stillingPeriode(ENHET_B, fom, tom, ENHET_B.getAgressoId())
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A.getId(), fom, tom, false),
            orgtilknytning("A000001", ENHET_B.getId(), fom, tom, false)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_B.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_A.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match historisk SP - ingen match mot eksisterende OT - match mot Orgenhet på ressurs (OPR) - ref: 1.ii.c")
    void test_matchHistoriskSP_ingenMatchMotEksisterendeOT_matchMotOPR__1_ii_c() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A_0110021, fom, tom, "011001"),
            stillingPeriode(ENHET_B_0110010, fom, tom, "011001")
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A_0110021.getId(), fom, tom, false),
            orgtilknytning("A000001", ENHET_B_0110010.getId(), fom, tom, false)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_B_0110010.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_A_0110021.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match historisk SP - ingen match mot eksisterende OT - match mot Orgenhet på ressurs (OPR) - ref: 1.ii.d")
    void test_matchHistoriskSP_ingenMatchMotEksisterendeOT_matchMotOPR__1_ii_d() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_A.getAgressoId()),
            stillingPeriode(ENHET_B, fom, tom, ENHET_A.getAgressoId())
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A.getId(), fom, tom, false),
            orgtilknytning("A000001", ENHET_C.getId(), fom, tom, true)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match historisk SP - ingen match mot eksisterende OT - match mot Orgenhet på ressurs (OPR) - ref: 1.ii.e")
    void test_matchHistoriskSP_ingenMatchMotEksisterendeOT_matchMotOPR__1_ii_e() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_C.getAgressoId()),
            stillingPeriode(ENHET_B, fom, tom, ENHET_C.getAgressoId())
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A.getId(), fom, tom, false),
            orgtilknytning("A000001", ENHET_B.getId(), fom, tom, false)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertFalse(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(HISTORIC_PERIODE_KAN_IKKE_SETTE_DOL));
    }

    @Test
    @DisplayName("Match historisk SP - dobbel match mot eksisterende OT med DOL gir error - ref: 1.ii.f")
    void test_matchCurrentSP_duplikatMatchOt_matchMotOPR__1_ii_f() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_A.getAgressoId()),
            stillingPeriode(ENHET_B, fom, tom, ENHET_A.getAgressoId())
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A.getId(), fom, tom, true),
            orgtilknytning("A000001", ENHET_B.getId(), fom, tom, true)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(DUPLIKATE_EKSISTERENDE_ORGTILKNYNINGER_MED_DOL_I_HISTORISK_PERIODE));
    }

    @Test
    @DisplayName("Det finnes flere eksisterende OT med DOL - Gir Error - matcher heller ikke på OPR - Gir Feil nr 2 - ref: 1.ii.g")
    void test_matchCurrentSP_duplikatMatchOT_ingenMatchMotOPR__1_ii_g() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_C.getAgressoId()),
            stillingPeriode(ENHET_B, fom, tom, ENHET_C.getAgressoId())
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A.getId(), fom, tom, true),
            orgtilknytning("A000001", ENHET_B.getId(), fom, tom, true)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertFalse(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(2, result.errors().size());
        assertEquals(1, result.countOfErrorType(DUPLIKATE_EKSISTERENDE_ORGTILKNYNINGER_MED_DOL_I_HISTORISK_PERIODE));
        assertEquals(1, result.countOfErrorType(HISTORIC_PERIODE_KAN_IKKE_SETTE_DOL));
    }

    @Test
    @DisplayName("Det finnes ingen eksisterende OT - matcher heller ikke på OPR - Gir Feil - ref: 1.ii.h")
    void test_matchCurrentSP_ingenOT_ingenMatchMotOPR__1_ii_h() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_C.getAgressoId()),
            stillingPeriode(ENHET_B, fom, tom, ENHET_C.getAgressoId())
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertFalse(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(HISTORIC_PERIODE_KAN_IKKE_SETTE_DOL));
    }

    @Test
    @DisplayName("Det er hull i historisk periode på stillingPerioder - Gir Feil - ref: 1.ii.i")
    void hullIHistoriskPeriode__1_ii_i() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);
        LocalDate fom2 = LocalDate.of(2022, 2, 1);
        LocalDate tom2 = LocalDate.of(2022, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_C.getAgressoId()),
            stillingPeriode(ENHET_B, fom2, tom2, ENHET_C.getAgressoId())
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom2,               andreStillingPeriode.gyldigFom());
        assertEquals(tom2,               andreStillingPeriode.gyldigTom());
        assertTrue(andreStillingPeriode.erDagligOppfolging());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(HULL_I_STILLINGSPERIODE));
    }

    @Test
    @DisplayName("Match historisk SP - ingen match mot eksisterende OT - OPR delvis matcher flere SP - ref: 1.ii.j")
    void test_matchHistoriskSP_ingenMatchMotEksisterendeOT_delvisMatchMotOPRmotFlereSPer__1_ii_j() {
        LocalDate fom = LocalDate.of(2020, 1, 1);
        LocalDate tom = LocalDate.of(2021, 12, 31);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A_0110021, fom, tom, "01100"),
            stillingPeriode(ENHET_B_0110010, fom, tom, "01100")
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A_0110021.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B_0110010.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(MULTI_MATCH_ORGENHET_PAA_RESSURS));
    }


    @Test
    @DisplayName("Match current/aktiv SP - singel SP - ref: 2.i")
    void test_matchHistoriskSP_singleSP__2_i() {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, "Enhet X")
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(1, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match current/aktiv SP - match kun mot Orgenhet på ressurs (OPR) - ref: 2.ii")
    void test_matchCurrentSP_matchKunMotOPR__2_ii() {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_A.getAgressoId()),
            stillingPeriode(ENHET_B, fom, tom, ENHET_A.getAgressoId())
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match current/aktiv SP - match kun mot Orgenhet på ressurs (OPR) - ref: 2.iii")
    void test_matchCurrentSP_matchKunMotOPR__2_iii() {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_B.getAgressoId()),
            stillingPeriode(ENHET_B, fom, tom, ENHET_B.getAgressoId())
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A.getId(), fom, tom, true),
            orgtilknytning("A000001", ENHET_B.getId(), fom, tom, false)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_B.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_A.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match current/aktiv SP - match kun mot Orgenhet på ressurs (OPR) - ref: 2.iv")
    void test_matchCurrentSP_matchKunMotOPR__2_iv() {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A_0110021, fom, tom, "011001"),
            stillingPeriode(ENHET_B_0110010, fom, tom, "011001")
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_B_0110010.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_A_0110021.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match current/aktiv SP - ingen match mot Orgenhet på ressurs (OPR) - ref: 2.v")
    void test_matchCurrentSP_ingenMatchMotOPR__2_v() {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_C.getAgressoId()),
            stillingPeriode(ENHET_B, fom, tom, ENHET_C.getAgressoId())
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertFalse(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(CURRENT_PERIODE_KAN_IKKE_MATCHE_ORGENHET_PAA_RESSURS_FOR_DOL));
    }

    @Test
    @DisplayName("Match current SP - ingen match mot eksisterende OT - OPR delvis matcher flere SP - ref: 2.vi")
    void test_matchCurrentSP_ingenMatchMotEksisterendeOT_delvisMatchMotOPRmotFlereSPer__2_vi() {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A_0110021, fom, tom, "01100"),
            stillingPeriode(ENHET_B_0110010, fom, tom, "01100")
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A_0110021.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B_0110010.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(MULTI_MATCH_ORGENHET_PAA_RESSURS));
    }

    @Test
    @DisplayName("Match future SP - singel SP - ref: 3.i")
    void test_matchFutureSP_singleSP__3_i() {
        LocalDate fom = LocalDate.now().plusMonths(3);
        LocalDate tom = LocalDate.now().plusYears(2);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, "Enhet X")
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(1, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match future SP - match kun mot Orgenhet på ressurs (OPR) - ref: 3.ii")
    void test_matchFutureSP_matchKunMotOPR__3_ii() {
        LocalDate fom = LocalDate.now().plusMonths(3);
        LocalDate tom = LocalDate.now().plusYears(2);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_A.getAgressoId()),
            stillingPeriode(ENHET_B, fom, tom, ENHET_A.getAgressoId())
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match future SP - det finnes OT med DOL - skal uansett matche på OPR - ref: 3.iii")
    void test_matchFutureSP_OTMedDOL_MatchUansettPaOPR__3_iii() {
        LocalDate fom = LocalDate.now().plusMonths(3);
        LocalDate tom = LocalDate.now().plusYears(2);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_B.getAgressoId()),
            stillingPeriode(ENHET_B, fom, tom, ENHET_B.getAgressoId())
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(List.of(
            orgtilknytning("A000001", ENHET_A.getId(), fom, tom, true),
            orgtilknytning("A000001", ENHET_B.getId(), fom, tom, false)
        ));

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_B.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_A.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match future SP - match kun mot Orgenhet på ressurs (OPR) - ref: 3.iv")
    void test_matchFutureSP_matchKunMotOPR__3_iv() {
        LocalDate fom = LocalDate.now().plusMonths(3);
        LocalDate tom = LocalDate.now().plusYears(2);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A_0110021, fom, tom, "011001"),
            stillingPeriode(ENHET_B_0110010, fom, tom, "011001")
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_B_0110010.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_A_0110021.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Match future SP - ingen match mot Orgenhet på ressurs (OPR) - ref: 3.v")
    void test_matchFutureSP_ingenMatchMotOPR__3_v() {
        LocalDate fom = LocalDate.now().plusMonths(3);
        LocalDate tom = MAX_DATE;

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_C.getAgressoId()),
            stillingPeriode(ENHET_B, fom, tom, ENHET_C.getAgressoId())
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertFalse(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(FUTURE_PERIODE_KAN_IKKE_SETTE_DOL));
    }


    @Test
    @DisplayName("Det er hull i periode på fremtidige stillingPerioder - Gir Feil - ref: 3.vi")
    void hullIFremtidigPeriode__3_vi() {
        LocalDate fom = LocalDate.now().plusMonths(3);
        LocalDate tom = LocalDate.now().plusMonths(5);
        LocalDate fom2 = LocalDate.now().plusMonths(7);
        LocalDate tom2 = LocalDate.now().plusMonths(9);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A, fom, tom, ENHET_C.getAgressoId()),
            stillingPeriode(ENHET_B, fom2, tom2, ENHET_C.getAgressoId())
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom2,               andreStillingPeriode.gyldigFom());
        assertEquals(tom2,               andreStillingPeriode.gyldigTom());
        assertTrue(andreStillingPeriode.erDagligOppfolging());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(HULL_I_STILLINGSPERIODE));
    }

    @Test
    @DisplayName("Match future SP - ingen match mot eksisterende OT - OPR delvis matcher flere SP - ref: 3.vii")
    void test_matchFutureSP_ingenMatchMotEksisterendeOT_delvisMatchMotOPRMotFlereSPer__3_vii() {
        LocalDate fom = LocalDate.now().plusMonths(3);
        LocalDate tom = LocalDate.now().plusYears(2);

        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode(ENHET_A_0110021, fom, tom, "01100"),
            stillingPeriode(ENHET_B_0110010, fom, tom, "01100")
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);

        var result = CUT.behandle(sspForNavident);

        assertEquals(2, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode  = result.stillingPerioder().getLast();

        assertEquals(ENHET_A_0110021.getNavn(), forsteStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               forsteStillingPeriode.gyldigFom());
        assertEquals(tom,               forsteStillingPeriode.gyldigTom());
        assertTrue(forsteStillingPeriode.erDagligOppfolging());

        assertEquals(ENHET_B_0110010.getNavn(), andreStillingPeriode.stilling().enhetNavn());
        assertEquals(fom,               andreStillingPeriode.gyldigFom());
        assertEquals(tom,               andreStillingPeriode.gyldigTom());
        assertFalse(andreStillingPeriode.erDagligOppfolging());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(MULTI_MATCH_ORGENHET_PAA_RESSURS));
    }

    // --- Test Interne metoder ---
    @Test
    @DisplayName("Test finn brekk datoer - ingen OT'er")
    void test_finnBrekkDatoerIngenOT() {
        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2022, 11, 30)),
            stillingPeriode("A000001", "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
            stillingPeriode("A000001", "AGRID_3", "Enhet_3", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 12, 31))
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);
        var result = CUT.finnBrekkDatoer(sspForNavident);

        List<LocalDate> expected = List.of(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2022, 1, 1),
            LocalDate.of(2022, 5, 1),
            LocalDate.of(2022, 12, 1),
            LocalDate.of(2023, 1, 1)
        );

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test finn brekk datoer - med OT'er")
    void test_finnBrekkDatoerMedOT() {
        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2022, 11, 30)),
            stillingPeriode("A000001", "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
            stillingPeriode("A000001", "AGRID_3", "Enhet_3", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 12, 31))
        );

        List<OrgtilknytningState> orgtilknytninger = List.of(
            orgtilknytning("A000001", "Enhet_1", LocalDate.of(2020, 4, 1), LocalDate.of(2022, 9, 30)),
            orgtilknytning("A000001", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(orgtilknytninger);

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);
        var result = CUT.finnBrekkDatoer(sspForNavident);

        List<LocalDate> expected = List.of(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 4, 1),
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2022, 1, 1),
            LocalDate.of(2022, 5, 1),
            LocalDate.of(2022, 10, 1),
            LocalDate.of(2022, 12, 1),
            LocalDate.of(2023, 1, 1)
        );

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test splitt StillingPerioder på brekk datoer")
    void test_splittStillingPerioderPaaBrekkDatoer() {
        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2022, 11, 30)),
            stillingPeriode("A000001", "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
            stillingPeriode("A000001", "AGRID_3", "Enhet_3", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 12, 31)),
            stillingPeriode("A000001", "AGRID_4", "Enhet_4", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
        );

        List<LocalDate> brekkDatoer = List.of(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 4, 1),
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2022, 1, 1),
            LocalDate.of(2022, 5, 1),
            LocalDate.of(2022, 10, 1),
            LocalDate.of(2022, 12, 1),
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 2),
            LocalDate.of(2024, 12, 31),
            LocalDate.of(2025, 1, 1)
        );

        List<StillingPeriode> expected = List.of(
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 3, 31)),
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2020, 4, 1), LocalDate.of(2020, 12, 31)),
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 4, 30)),
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 9, 30)),
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 30)),
            stillingPeriode("A000001", "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
            stillingPeriode("A000001", "AGRID_3", "Enhet_3", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 9, 30)),
            stillingPeriode("A000001", "AGRID_3", "Enhet_3", LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 30)),
            stillingPeriode("A000001", "AGRID_3", "Enhet_3", LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 31)),
            stillingPeriode("A000001", "AGRID_4", "Enhet_4", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
            stillingPeriode("A000001", "AGRID_4", "Enhet_4", LocalDate.of(2024, 1, 2), LocalDate.of(2024, 12, 30)),
            stillingPeriode("A000001", "AGRID_4", "Enhet_4", LocalDate.of(2024, 12, 31), LocalDate.of(2024, 12, 31))
        );

        var result = CUT.splittStillingPerioder(stillingPerioder, brekkDatoer);

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test finn historic fomDates - no current Fom dates")
    void test_finnHistoricFomDates_noCurrent() {
        LocalDate now = LocalDate.now();

        List<LocalDate> fomDates = List.of(
            now.minusYears(1),
            now.minusMonths(3),
            now.minusMonths(1)
        );

        var result = CUT.historicFomDates(fomDates);

        List<LocalDate> expected = List.of(
            now.minusYears(1),
            now.minusMonths(3)
        );

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test finn historic fomDates - no current Fom dates - end at today")
    void test_finnHistoricFomDates_noCurrent_endToday() {
        LocalDate now = LocalDate.now();

        List<LocalDate> fomDates = List.of(
            now.minusYears(1),
            now.minusMonths(3),
            now
        );

        var result = CUT.historicFomDates(fomDates);

        List<LocalDate> expected = List.of(
            now.minusYears(1),
            now.minusMonths(3)
        );

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test finn historic fomDates - with current and future dates")
    void test_finnHistoricFomDates_currentAndFutureDates() {
        LocalDate now = LocalDate.now();

        List<LocalDate> fomDates = List.of(
            now.minusYears(1),
            now.minusMonths(3),
            now.minusMonths(1),
            now.plusMonths(1),
            now.plusYears(1)
        );

        var result = CUT.historicFomDates(fomDates);

        List<LocalDate> expected = List.of(
            now.minusYears(1),
            now.minusMonths(3)
        );

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test finn historic fomDates - only future dates")
    void test_finnHistoricFomDates_onlyFutureDates() {
        LocalDate now = LocalDate.now();

        List<LocalDate> fomDates = List.of(
            now,
            now.plusMonths(1),
            now.plusYears(1)
        );

        var result = CUT.historicFomDates(fomDates);

        List<LocalDate> expected = List.of();

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test finn current fomDates")
    void test_finnCurrentFomDates() {
        LocalDate now = LocalDate.now();

        List<LocalDate> fomDates = List.of(
            now.minusYears(1),
            now.minusMonths(3),
            now.minusMonths(1)
        );

        var result = CUT.currentFomDate(fomDates);
        assertEquals(now.minusMonths(1) , result);

        fomDates = List.of(
            now.minusYears(1),
            now.minusMonths(3),
            now
        );

        result = CUT.currentFomDate(fomDates);
        assertEquals(now, result);

        result = CUT.currentFomDate(List.of());
        assertNull(result);

        fomDates = List.of(
            now.plusMonths(3),
            now.plusYears(1)
        );

        result = CUT.currentFomDate(fomDates);
        assertNull(result);
    }

    @Test
    @DisplayName("Test finn future fomDates")
    void test_finnFutureFomDates() {
        LocalDate now = LocalDate.now();

        List<LocalDate> fomDates = List.of(
            now.minusYears(1),
            now.minusMonths(3),
            now
        );

        var result = CUT.futureFomDates(fomDates);
        assertEquals(List.of() , result);

        fomDates = List.of(
            now.minusYears(1),
            now.minusMonths(3),
            now.minusMonths(1),
            now.plusMonths(1),
            now.plusYears(1)
        );

        List<LocalDate> expected = List.of(
            now.plusMonths(1),
            now.plusYears(1)
        );

        result = CUT.futureFomDates(fomDates);
        assertEquals(expected, result);
    }

    IOrgenhet ENHET_A = new OrgEnhet(
        new OrgEnhetState(
            "FID_A", "a100xy", "Enhet A",
            LocalDate.of(2015, 1, 1), null,
            null, null, null, null, null,
            "Enhet_A", "ORGENHET", null
        )
    );
    IOrgenhet ENHET_B = new OrgEnhet(
        new OrgEnhetState(
            "FID_B", "b100xy", "Enhet B",
            LocalDate.of(2015, 1, 1), null,
            null, null, null, null, null,
            "Enhet_B", "ORGENHET", null
        )
    );
    IOrgenhet ENHET_C = new OrgEnhet(
        new OrgEnhetState(
            "FID_C", "c100xy", "Enhet C",
            LocalDate.of(2015, 1, 1), null,
            null, null, null, null, null,
            "Enhet_C", "ORGENHET", null
        )
    );

    IOrgenhet ENHET_A_0110021 = new OrgEnhet(
        new OrgEnhetState(
            "FID_A", "a100xy", "Enhet A",
            LocalDate.of(2015, 1, 1), null,
            null, null, null, null, null,
            "0110021", "ORGENHET", null
        )
    );
    IOrgenhet ENHET_B_0110010 = new OrgEnhet(
        new OrgEnhetState(
            "FID_B", "b100xy", "Enhet B",
            LocalDate.of(2015, 1, 1), null,
            null, null, null, null, null,
            "0110010", "ORGENHET", null
        )
    );
}