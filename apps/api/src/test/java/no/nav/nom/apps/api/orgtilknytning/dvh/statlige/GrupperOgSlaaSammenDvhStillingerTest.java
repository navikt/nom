package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.DVH_RESSURSSTILLINGER_HAR_OVERLAPPENDE_PERIODER;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

// REF: no/nav/nom/apps/api/orgtilknytning/dvh/dokumentasjon/TestReferanser_GrupperOgSlaaSammenPerioder.md
@DisplayName("Tester gruppering og strukturering av periodisering på RessursStillinger fra DVH")
class GrupperOgSlaaSammenDvhStillingerTest extends TestBase {
    @Autowired
    GrupperOgSlaaSammenDvhStillinger CUT; // Class under test
    @Autowired
    Unleash unleash;

    @Test
    @DisplayName("Basic sammenslåing")
    void test_BasicSammenslaaingAvDvhRessursStillinger() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.i
    @DisplayName("Splitt på ikke sammenhengende periode")
    public void test_SplittPaaIkkeSammenhengendePeriode() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 8, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();

        assertEquals(2, result.stillingPerioder().size());

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());

        assertEquals("AGRID_1", andreStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_1", andreStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", andreStillingPeriode.stilling().ressursAgressoOrgNiv());

        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2020, 8, 31), forsteStillingPeriode.gyldigTom());

        assertEquals(LocalDate.of(2020, 11, 1), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, andreStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.ii
    @DisplayName("Splitt på ulike enheter")
    public void test_SplittPaaUlikeEnheter() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", "AGRID_x", "ORGENHET", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), null),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", "AGRID_x", "ORGENHET", LocalDate.of(2022, 1, 1), MAX_DATE, null)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();

        assertEquals(2, result.stillingPerioder().size());

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2020, 10, 31), forsteStillingPeriode.gyldigTom());

        assertEquals("AGRID_2", andreStillingPeriode.stilling().agressoId());
        assertEquals(LocalDate.of(2020, 11, 1), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, andreStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.iii
    @DisplayName("Splitt på ulike 'OrgenhetPåRessurs' selv om 'AgressoId' er lik")
    public void test_SplittPaaUlikeOrgenhetPaaRessurs() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AGRID_x", "ORGENHET", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), null),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AGRID_x", "ORGENHET", LocalDate.of(2022, 1, 1), MAX_DATE, null)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();

        assertEquals(2, result.stillingPerioder().size());

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().ressursAgressoId());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2020, 10, 31), forsteStillingPeriode.gyldigTom());

        assertEquals("AGRID_1", andreStillingPeriode.stilling().agressoId());
        assertEquals("AGRID_x", andreStillingPeriode.stilling().ressursAgressoId());
        assertEquals(LocalDate.of(2020, 11, 1), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, andreStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.iv
    @DisplayName("AgressoId presis, men orgenhet på ressurs er upresis")
    void test_PresisAgressoID_UpresisOrgenhetPaaRessurs_IderErMatch() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AGRID", "ORGNIV3", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), null),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AGRID", "ORGNIV3", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), null),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AGRID", "ORGNIV3", LocalDate.of(2022, 1, 1), MAX_DATE, null)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals("AGRID", forsteStillingPeriode.stilling().ressursAgressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGNIV3", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.v
    @DisplayName("AgressoId er upresis, men orgenhet på ressurs er presis")
    void test_UpresisAgressoID_PresisOrgenhetPaaRessurs_IderErMatch() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID", "Enhet_1", "AGRID_1", "ORGENHET", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), null),
            dvhRessursStilling(A000001, "AGRID", "Enhet_1", "AGRID_1", "ORGENHET", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), null),
            dvhRessursStilling(A000001, "AGRID", "Enhet_1", "AGRID_1", "ORGENHET", LocalDate.of(2022, 1, 1), MAX_DATE, null)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID", forsteStillingPeriode.stilling().agressoId());
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().ressursAgressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.vi
    @DisplayName("AgressoId presis, men orgenhet på ressurs er upresis - Id'ene matcher ikke")
    void test_PresisAgressoID_UpresisOrgenhetPaaRessurs_IderMatcherIkke() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AID_2", "ORGENHET", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), null),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AID_2", "ORGENHET", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), null),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AID_2", "ORGENHET", LocalDate.of(2022, 1, 1), MAX_DATE, null)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals("AID_2", forsteStillingPeriode.stilling().ressursAgressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.vii
    @DisplayName("Både AgressoId og orgenhet på ressurs er upresis - Id'ene matcher")
    void test_UpresisAgressoID_PresisOrgenhetPaaRessurs_IderMatcher() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_1", "ORGNIV3", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), null),
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_1", "ORGNIV3", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), null),
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_1", "ORGNIV3", LocalDate.of(2022, 1, 1), MAX_DATE, null)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AID_100", forsteStillingPeriode.stilling().agressoId());
        assertEquals("AID_1", forsteStillingPeriode.stilling().ressursAgressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGNIV3", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.viii
    @DisplayName("Både AgressoId og orgenhet på ressurs er upresis - Id'ene matcher ikke")
    void test_UpresisAgressoID_UpresisOrgenhetPaaRessurs_IderMatcherIkke() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_2", "ORGNIV3", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), null),
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_2", "ORGNIV3", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), null),
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_2", "ORGNIV3", LocalDate.of(2022, 1, 1), MAX_DATE, null)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AID_100", forsteStillingPeriode.stilling().agressoId());
        assertEquals("AID_2", forsteStillingPeriode.stilling().ressursAgressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGNIV3", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.ix
    @DisplayName("AgressoId mangler, men orgenhet på ressurs er presis")
    void test_ManglerAgressoID_PresisOrgenhetPaaRessurs() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "-4", "Mangler", "AGRID_1", "ORGENHET", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), null),
            dvhRessursStilling(A000001, "-4", "Mangler", "AGRID_1", "ORGENHET", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), null),
            dvhRessursStilling(A000001, "-4", "Mangler", "AGRID_1", "ORGENHET", LocalDate.of(2022, 1, 1), MAX_DATE, null)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("-4", forsteStillingPeriode.stilling().agressoId());
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().ressursAgressoId());
        assertEquals("Mangler", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.x
    @DisplayName("AgressoId mangler, og orgenhet på ressurs er upresis")
    void test_ManglerAgressoID_UpresisOrgenhetPaaRessurs() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "-4", "Mangler", "AID_1", "ORGNIV3", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), null),
            dvhRessursStilling(A000001, "-4", "Mangler", "AID_1", "ORGNIV3", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), null),
            dvhRessursStilling(A000001, "-4", "Mangler", "AID_1", "ORGNIV3", LocalDate.of(2022, 1, 1), MAX_DATE, null)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("-4", forsteStillingPeriode.stilling().agressoId());
        assertEquals("AID_1", forsteStillingPeriode.stilling().ressursAgressoId());
        assertEquals("Mangler", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGNIV3", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.xi
    @DisplayName("Mangler både AgressoId og orgenhet på ressurs")
    void test_ManglerAgressoID_ManglerOrgenhetPaaRessurs() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "-4", "Mangler", "-3", null, LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), null),
            dvhRessursStilling(A000001, "-4", "Mangler", "-3", null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), null),
            dvhRessursStilling(A000001, "-4", "Mangler", "-3", null, LocalDate.of(2022, 1, 1), MAX_DATE, null)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("-4", forsteStillingPeriode.stilling().agressoId());
        assertEquals("-3", forsteStillingPeriode.stilling().ressursAgressoId());
        assertEquals("Mangler", forsteStillingPeriode.stilling().enhetNavn());
        assertNull(forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 2.i
    @DisplayName("Perioder henger sammen, og første periode slutter før ressursens startdato")
    void test_FoerstePeriodeSlutterFoerRessursensStartDato() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2020, 11, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 11, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());

        assertEquals(1, result.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getFirst().errorType());
    }

    @Test
    // Test referanser 2.ii
    @DisplayName("Ulike perioder hvor første periode starter før ressursen startdato, men slutter etter")
    void test_FoerstePeriodeStarterFoerRessursensStartDato() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2020, 5, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 5, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());

        assertEquals(1, result.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getFirst().errorType());
    }

    @Test
    // Test referanser 2.iii
    @DisplayName("Ulike perioder henger sammen, og første periode slutter før ressursens startdato, den andre starter før ressursens startdato")
    void test_UlikePerioder_FoersteMedPeriodeFoerOgEnSomStarterFoerRessursensStartDato() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2021, 11, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2021, 11, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2021, 12, 31), forsteStillingPeriode.gyldigTom());

        assertEquals("AGRID_2", andreStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_2", andreStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", andreStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2022, 1, 1), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, andreStillingPeriode.gyldigTom());

        assertEquals(2, result.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getFirst().errorType());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getLast().errorType());
    }

    @Test
    // Test referanser 3.i
    @DisplayName("Perioder henger sammen, og siste periode slutter etter ressursens sluttdato")
    void test_SistePeriodeSlutterEtterRessursensStartDato() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2019, 1, 1), LocalDate.of(2023, 12, 31))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2023, 12, 31), forsteStillingPeriode.gyldigTom());

        assertEquals(1, result.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getFirst().errorType());
    }

    @Test
    // Test referanser 3.ii
    @DisplayName("Perioder henger sammen, og siste periode starter etter ressursens sluttdato")
    void test_SistePeriodeStarterEtterRessursensStartDato() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2019, 1, 1), LocalDate.of(2021, 12, 31))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2021, 12, 31), forsteStillingPeriode.gyldigTom());

        assertEquals(1, result.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getFirst().errorType());
    }

    @Test
    // Test referanser 3.iii
    @DisplayName("Ulike perioder henger sammen, den ene med periode som slutter etter ressursen sluttdato, den andre med periode etter ressursen sluttdato")
    void test_UlikePerioder_FoersteMedPeriodeSlutterEtterRessursensSluttDato_AndreStarterEtterRessursenSluttdato() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2019, 1, 1), LocalDate.of(2021, 8, 31))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2021, 8, 31), forsteStillingPeriode.gyldigTom());

        assertEquals(2, result.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getFirst().errorType());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getLast().errorType());
    }

    @Test
    // Test referanser 4.i
    @DisplayName("Like enheter, men overlapp av periode")
    void test_OverlappAvPeriode() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 3, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        var forsteStillingPeriode = result.stillingPerioder().getFirst();

        assertEquals(1, result.stillingPerioder().size());

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().ressursAgressoId());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());

        assertEquals(1, result.errors().size());
        assertEquals(DVH_RESSURSSTILLINGER_HAR_OVERLAPPENDE_PERIODER, result.errors().getFirst().errorType());
    }


    @Test
    // Test referanser 4.ii
    @DisplayName("Splitt på ulike enheter med overlapp av periode - Flere overlapp")
    void test_SplittPaaUlikeEnheterMedOverlapp_FlereOverlapp() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 3, 1), MAX_DATE), // Overlapper den over
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), MAX_DATE), // Overlapper den under
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31))
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();

        assertEquals(2, result.stillingPerioder().size());

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().ressursAgressoId());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());

        assertEquals("AGRID_2", andreStillingPeriode.stilling().ressursAgressoId());
        assertEquals(LocalDate.of(2020, 11, 1), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, andreStillingPeriode.gyldigTom());

        assertEquals(2, result.errors().size());
        assertEquals(DVH_RESSURSSTILLINGER_HAR_OVERLAPPENDE_PERIODER, result.errors().getFirst().errorType());
        assertEquals(DVH_RESSURSSTILLINGER_HAR_OVERLAPPENDE_PERIODER, result.errors().getFirst().errorType());
    }

    @Test
    // Test referanser 5
    @DisplayName("Splitt på ulike sektor og tilknytninger, men sammenhengende datoer")
    void test_splittPaaUlikeSektorMedSammenhengendeDatoer() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), "Midlertidig karakter § 9a"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 6, 15), "Midlertidig karakter § 9a"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 6, 16), MAX_DATE, "Fast")
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2021, 6, 15), forsteStillingPeriode.gyldigTom());
        assertEquals(Sektor.NAV_STATLIG, forsteStillingPeriode.sektor());
        assertEquals(Tilknytningtype.MIDLERTIDIG, forsteStillingPeriode.tilknytningtype());
        var andreStillingPeriode = result.stillingPerioder().getLast();
        assertEquals("AGRID_1", andreStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_1", andreStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", andreStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2021, 6, 16), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, andreStillingPeriode.gyldigTom());
        assertEquals(Sektor.NAV_STATLIG, andreStillingPeriode.sektor());
        assertEquals(Tilknytningtype.FAST, andreStillingPeriode.tilknytningtype());
    }

    @Test
    // Test referanser 6
    @DisplayName("Slå sammen sammenhengende perioder med like sektor og tilknytninger")
    void test_SlaaSammenSammenhengendePerioderMedLikeSektorOgTilknytninger() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 6, 15), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 6, 16), MAX_DATE, "Fast")
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
        assertEquals(Sektor.NAV_STATLIG, forsteStillingPeriode.sektor());
        assertEquals(Tilknytningtype.FAST, forsteStillingPeriode.tilknytningtype());
    }

    @Test
    @DisplayName("Ikke slå sammen sammenhegende perioder med ulike sektor og tilknytninger")
    void test_IkkeSlaaSammenSammenhengendePerioderMedUlikeSektorOgTilknytninger() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), "Midlertidig karakter § 9a"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 6, 15), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 6, 16), MAX_DATE, "Midlertidig karakter § 9a")
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(3, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().get(1);
        var tredjeStillingPeriode = result.stillingPerioder().getLast();

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2020, 12, 31), forsteStillingPeriode.gyldigTom());
        assertEquals(Sektor.NAV_STATLIG, forsteStillingPeriode.sektor());
        assertEquals(Tilknytningtype.MIDLERTIDIG, forsteStillingPeriode.tilknytningtype());
        assertEquals("AGRID_1", andreStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_1", andreStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", andreStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2021, 1, 1), andreStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2021, 6, 15), andreStillingPeriode.gyldigTom());
        assertEquals(Sektor.NAV_STATLIG, andreStillingPeriode.sektor());
        assertEquals(Tilknytningtype.FAST, andreStillingPeriode.tilknytningtype());
        assertEquals("AGRID_1", tredjeStillingPeriode.stilling().agressoId());
        assertEquals("Enhet_1", tredjeStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", tredjeStillingPeriode.stilling().ressursAgressoOrgNiv());
        assertEquals(LocalDate.of(2021, 6, 16), tredjeStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, tredjeStillingPeriode.gyldigTom());
        assertEquals(Sektor.NAV_STATLIG, tredjeStillingPeriode.sektor());
        assertEquals(Tilknytningtype.MIDLERTIDIG, tredjeStillingPeriode.tilknytningtype());
    }

    @Test
    @DisplayName("Opprett stillingperioder med lik dato, men ulike mappede tilknytningtyper")
    void test_StillingPerioderMedBareMappedeTilknytningtyperOgLikPeriode() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), "Midlertidig karakter § 9a"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), "Utdanningsstillinger § 9d")
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(4, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().get(1);
        var tredjeStillingPeriode = result.stillingPerioder().get(2);
        var fjerdeStillingPeriode = result.stillingPerioder().getLast();

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals(Tilknytningtype.FAST, forsteStillingPeriode.tilknytningtype());
        assertEquals("AGRID_1", andreStillingPeriode.stilling().agressoId());
        assertEquals(Tilknytningtype.MIDLERTIDIG, andreStillingPeriode.tilknytningtype());
        assertEquals("AGRID_2", tredjeStillingPeriode.stilling().agressoId());
        assertEquals(Tilknytningtype.FAST, tredjeStillingPeriode.tilknytningtype());
        assertEquals("AGRID_2", fjerdeStillingPeriode.stilling().agressoId());
        assertEquals(Tilknytningtype.UTDANNINGSSTILLINGER, fjerdeStillingPeriode.tilknytningtype());
    }

    @Test
    @DisplayName("Opprett stillingperioder med lik dato, men med mappede og ikke mappede tilknytningtyper")
    void test_StillingPerioderMedMappedeOgIkkeMappedeTilknytningtyperMedLikPeriode() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), "Uforutsett behov § 9e"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), "Timelønn")
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals(Tilknytningtype.FAST, forsteStillingPeriode.tilknytningtype());
        assertEquals("AGRID_2", andreStillingPeriode.stilling().agressoId());
        assertEquals(Tilknytningtype.MIDLERTIDIG, andreStillingPeriode.tilknytningtype());
    }

    @Test
    @DisplayName("Opprett stillingperioder hvor en periode dekker hele andre perioden, hvor den større perioden har tilknytningstype")
    void test_StillingPeriodeMedTilknytningPaaDenSomHarLengstPeriode() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), "Timelønn")
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId());
        assertEquals(Tilknytningtype.FAST, forsteStillingPeriode.tilknytningtype());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 12, 31), forsteStillingPeriode.gyldigTom());
    }

    @Test
    @DisplayName("Opprett stillingperioder hvor en periode dekker hele andre perioden, hvor den mindre perioden har tilknytningstype")
    void test_StillingPeriodeMedTilknytningPaaDenSomHarKortestPeriode() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), "Timelønn"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), "Timelønn"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31), "Timelønn"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), "Fast")
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(3, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().get(1);
        var tredjeStillingPeriode = result.stillingPerioder().getLast();

        assertAll(
            () -> assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId()),
            () -> assertNull(forsteStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2020, 12, 31), forsteStillingPeriode.gyldigTom()),
            () -> assertEquals("AGRID_1", andreStillingPeriode.stilling().agressoId()),
            () -> assertEquals(Tilknytningtype.FAST, andreStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2021, 1, 1), andreStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2021, 12, 31), andreStillingPeriode.gyldigTom()),
            () -> assertEquals("AGRID_1", tredjeStillingPeriode.stilling().agressoId()),
            () -> assertNull(tredjeStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2022, 1, 1), tredjeStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2022, 12, 31), tredjeStillingPeriode.gyldigTom())
        );
    }

    @Test
    @DisplayName("To stillingperioder uten tilknytning som starter før og nummer to som slutter etter stillingperioden med tilknytning")
    void test_ToStillingPerioderUtenTilknytningSomStarterFoerOgSlutterEtterStillingPeriodeMedTilknytning() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 11, 30)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 7, 1), LocalDate.of(2021, 6, 30), "Fast")
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(3, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().get(1);
        var tredjeStillingPeriode = result.stillingPerioder().getLast();

        assertAll(
            () -> assertNull(forsteStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2020, 6, 30), forsteStillingPeriode.gyldigTom()),
            () -> assertEquals(Tilknytningtype.FAST, andreStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2020, 7, 1), andreStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2021, 6, 30), andreStillingPeriode.gyldigTom()),
            () -> assertNull(tredjeStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2021, 7, 1), tredjeStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2021, 12, 31), tredjeStillingPeriode.gyldigTom())
        );
    }

    @Test
    @DisplayName("Tre stillingperioder uten tilknytning som starter før og slutter etter to stillingperioder med tilknytning")
    void test_TreStillingPerioderUtenTilknytningSomStarterFoerOgSlutterEtterToStillingPerioderMedTilknytning() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 7, 1), LocalDate.of(2021, 5, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 7, 1), LocalDate.of(2022, 6, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 10, 31))
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(5, result.stillingPerioder().size());

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().get(1);
        var tredjeStillingPeriode = result.stillingPerioder().get(2);
        var fjerdeStillingPeriode = result.stillingPerioder().get(3);
        var femteStillingPeriode = result.stillingPerioder().getLast();

        assertAll(
            () -> assertNull(forsteStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2020, 6, 30), forsteStillingPeriode.gyldigTom()),
            () -> assertEquals(Tilknytningtype.FAST, andreStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2020, 7, 1), andreStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2021, 5, 31), andreStillingPeriode.gyldigTom()),
            () -> assertNull(tredjeStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2021, 6, 1), tredjeStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2021, 6, 30), tredjeStillingPeriode.gyldigTom()),
            () -> assertEquals(Tilknytningtype.FAST, fjerdeStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2021, 7, 1), fjerdeStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2022, 6, 30), fjerdeStillingPeriode.gyldigTom()),
            () -> assertNull(femteStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2022, 7, 1), femteStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2022, 10, 31), femteStillingPeriode.gyldigTom())
        );
    }

    @Test
    @DisplayName("Får null i perioderGruppert")
    void test_FaarNullIPerioderGruppert() {
        List<DvhRessursStilling> dvhRessursStillinger = Collections.emptyList();

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(result.stillingPerioder(), Collections.emptyList());
    }

    @Test
    @DisplayName("Test case fra DEV Miljø hvor ressursen ender opp med to like OTer, men forskjellige perioder")
    void test_ToLikeOTerMedDelvisOverlappendePerioder() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 4, 1), LocalDate.of(2022, 4, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 5, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 6, 1), LocalDate.of(2022, 6, 1), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 6, 2), LocalDate.of(2022, 7, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 8), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 8, 9), LocalDate.of(2022, 9, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 10, 1), LocalDate.of(2022, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 4, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 5, 1), LocalDate.of(2023, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 4, 1), LocalDate.of(2024, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 5, 1), LocalDate.of(2024, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 6, 1), LocalDate.of(2024, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 9, 1), LocalDate.of(2024, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 11, 1), LocalDate.of(2024, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 2, 1), LocalDate.of(2025, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 5, 1), LocalDate.of(2025, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2026, 1, 1), LocalDate.of(2099, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 4, 1), LocalDate.of(2022, 4, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 5, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 6, 1), LocalDate.of(2022, 6, 1), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 6, 2), LocalDate.of(2022, 7, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 8), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 8, 9), LocalDate.of(2022, 9, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 10, 1), LocalDate.of(2022, 12, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 4, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 5, 1), LocalDate.of(2023, 12, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 6, 1), LocalDate.of(2024, 8, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 9, 1), LocalDate.of(2024, 10, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 11, 1), LocalDate.of(2024, 12, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 4, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 5, 1), LocalDate.of(2025, 12, 31), "Innlån/utlån")
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));

        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);
        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(3, result.stillingPerioder().size());
        var stillingPeriode1 = result.stillingPerioder().getFirst();
        var stillingPeriode2 = result.stillingPerioder().get(1);
        var stillingPeriode3 = result.stillingPerioder().getLast();

        assertAll(
            () -> assertEquals(stillingPeriode1.gyldigFom(), LocalDate.of(2022, 4, 1)),
            () -> assertEquals(stillingPeriode1.gyldigTom(), LocalDate.of(2022, 5, 31)),
            () -> assertEquals(Tilknytningtype.FAST, stillingPeriode1.tilknytningtype()),
            () -> assertEquals(stillingPeriode2.gyldigFom(), LocalDate.of(2022, 6, 1)),
            () -> assertEquals(stillingPeriode2.gyldigTom(), LocalDate.of(2022, 7, 31)),
            () -> assertEquals(Tilknytningtype.FAST, stillingPeriode2.tilknytningtype()),
            () -> assertEquals(stillingPeriode3.gyldigFom(), LocalDate.of(2022, 8, 1)),
            () -> assertEquals(stillingPeriode3.gyldigTom(), LocalDate.of(2099, 12, 31)),
            () -> assertEquals(Tilknytningtype.FAST, stillingPeriode3.tilknytningtype())
        );
    }

    @Test
    @DisplayName("Test case with provided data")
    void test_ProvidedData() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 4, 1), LocalDate.of(2022, 4, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 4, 1), LocalDate.of(2022, 4, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 12, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 4, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 4, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 5, 1), LocalDate.of(2023, 11, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 5, 1), LocalDate.of(2023, 11, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 12, 1), LocalDate.of(2023, 12, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 12, 1), LocalDate.of(2023, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 5, 1), LocalDate.of(2024, 7, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 5, 1), LocalDate.of(2024, 7, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 14), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 14), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 8, 15), LocalDate.of(2024, 8, 31), "Fungering"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 8, 15), LocalDate.of(2024, 8, 31), "Fungering"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 9, 1), LocalDate.of(2024, 9, 30), "Fungering"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 9, 1), LocalDate.of(2024, 9, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 31), "Fungering"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 11, 1), LocalDate.of(2024, 11, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 11, 1), LocalDate.of(2024, 11, 30), "Fungering"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 12, 1), LocalDate.of(2025, 1, 31), "Fungering"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 12, 1), LocalDate.of(2025, 1, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 2, 1), LocalDate.of(2025, 4, 30), "Fungering"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 2, 1), LocalDate.of(2025, 4, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 5, 1), LocalDate.of(2099, 12, 31), "Fast")
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(3, result.stillingPerioder().size());

        var stillingPeriode1 = result.stillingPerioder().getFirst();
        var stillingPeriode2 = result.stillingPerioder().get(1);
        var stillingPeriode3 = result.stillingPerioder().getLast();

        assertAll(
            () -> assertEquals(stillingPeriode1.gyldigFom(), LocalDate.of(2022, 4, 1)),
            () -> assertEquals(stillingPeriode1.gyldigTom(), LocalDate.of(2024, 8, 14)),
            () -> assertEquals(Tilknytningtype.FAST, stillingPeriode1.tilknytningtype()),
            () -> assertEquals(stillingPeriode2.gyldigFom(), LocalDate.of(2024, 8, 15)),
            () -> assertEquals(stillingPeriode2.gyldigTom(), LocalDate.of(2024, 8, 31)),
            () -> assertNull(stillingPeriode2.tilknytningtype()),
            () -> assertEquals(stillingPeriode3.gyldigFom(), LocalDate.of(2024, 9, 1)),
            () -> assertEquals(stillingPeriode3.gyldigTom(), LocalDate.of(2099, 12, 31)),
            () -> assertEquals(Tilknytningtype.FAST, stillingPeriode3.tilknytningtype())
        );
    }

    @Test
    @DisplayName("Test case with provided data for navident S108639")
    void test_ProvidedDataForNavidentS108639() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2022, 4, 1), LocalDate.of(2022, 4, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 5, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 5, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2022, 6, 1), LocalDate.of(2022, 6, 8), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2022, 6, 9), LocalDate.of(2022, 7, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2022, 8, 1), LocalDate.of(2022, 12, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2022, 8, 1), LocalDate.of(2022, 12, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 4, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 4, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2023, 6, 1), LocalDate.of(2023, 12, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2023, 6, 1), LocalDate.of(2023, 12, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 5, 1), LocalDate.of(2024, 6, 1), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 5, 1), LocalDate.of(2024, 6, 1), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 6, 2), LocalDate.of(2024, 6, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 6, 2), LocalDate.of(2024, 6, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 8, 1), LocalDate.of(2024, 12, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 8, 1), LocalDate.of(2024, 12, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2025, 2, 1), LocalDate.of(2025, 3, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 30), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2025, 5, 1), LocalDate.of(2025, 12, 31), "Innlån/utlån"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2022, 8, 1), LocalDate.of(2022, 12, 31), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 4, 30), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 31), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2023, 6, 1), LocalDate.of(2023, 12, 31), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 31), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 5, 1), LocalDate.of(2024, 6, 1), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 6, 2), LocalDate.of(2024, 6, 30), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 31), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2024, 8, 1), LocalDate.of(2024, 12, 31), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2025, 2, 1), LocalDate.of(2025, 3, 31), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 30), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2025, 5, 1), LocalDate.of(2025, 12, 31), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 31), "Vikar § 9b"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2022, 6, 1), LocalDate.of(2022, 6, 8), "Fungering"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2022, 6, 1), LocalDate.of(2022, 6, 8), "Fungering"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2022, 6, 9), LocalDate.of(2022, 7, 31), "Fungering"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2022, 6, 9), LocalDate.of(2022, 7, 31), "Fungering"),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet 1", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 5, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2022, 4, 1), LocalDate.of(2022, 4, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2022, 6, 1), LocalDate.of(2022, 6, 8), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2022, 6, 9), LocalDate.of(2022, 7, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2022, 8, 1), LocalDate.of(2022, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 4, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2023, 6, 1), LocalDate.of(2023, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2024, 5, 1), LocalDate.of(2024, 6, 1), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2024, 6, 2), LocalDate.of(2024, 6, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2024, 8, 1), LocalDate.of(2024, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2025, 2, 1), LocalDate.of(2025, 3, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 30), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2025, 5, 1), LocalDate.of(2025, 12, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 31), "Fast"),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet 2", LocalDate.of(2026, 8, 1), LocalDate.of(2099, 12, 31), "Fast")
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2022, 4, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(6, result.stillingPerioder().size());
    }
}