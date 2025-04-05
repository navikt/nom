package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Basic sammenslåing")
    void test_BasicSammenslaaingAvDvhRessursStillinger(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 1.i
    @DisplayName("Splitt på ikke sammenhengende periode")
    public void test_SplittPaaIkkeSammenhengendePeriode(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 8, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();

        assertEquals(2, result.stillingPerioder().size());

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));

        assertEquals("AGRID_1", andreStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1", andreStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", andreStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));

        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2020, 8, 31), forsteStillingPeriode.gyldigTom());

        assertEquals(LocalDate.of(2020, 11, 1), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, andreStillingPeriode.gyldigTom());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 1.ii
    @DisplayName("Splitt på ulike enheter")
    public void test_SplittPaaUlikeEnheter(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", "AGRID_x", "ORGENHET", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), Optional.empty()),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", "AGRID_x", "ORGENHET", LocalDate.of(2022, 1, 1), MAX_DATE, Optional.empty())
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();

        assertEquals(2, result.stillingPerioder().size());

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2020, 10, 31), forsteStillingPeriode.gyldigTom());

        assertEquals("AGRID_2", andreStillingPeriode.stilling().agressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 11, 1), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, andreStillingPeriode.gyldigTom());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 1.iii
    @DisplayName("Splitt på ulike 'OrgenhetPåRessurs' selv om 'AgressoId' er lik")
    public void test_SplittPaaUlikeOrgenhetPaaRessurs(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AGRID_x", "ORGENHET", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), Optional.empty()),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AGRID_x", "ORGENHET", LocalDate.of(2022, 1, 1), MAX_DATE, Optional.empty())
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();

        assertEquals(2, result.stillingPerioder().size());

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse("NULL"));
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2020, 10, 31), forsteStillingPeriode.gyldigTom());

        assertEquals("AGRID_1", andreStillingPeriode.stilling().agressoId().orElse("NULL"));
        assertEquals("AGRID_x", andreStillingPeriode.stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 11, 1), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, andreStillingPeriode.gyldigTom());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 1.iv
    @DisplayName("AgressoId presis, men orgenhet på ressurs er upresis")
    void test_PresisAgressoID_UpresisOrgenhetPaaRessurs_IderErMatch(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AGRID", "ORGNIV3", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), Optional.empty()),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AGRID", "ORGNIV3", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), Optional.empty()),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AGRID", "ORGNIV3", LocalDate.of(2022, 1, 1), MAX_DATE, Optional.empty())
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("AGRID", forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGNIV3", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 1.v
    @DisplayName("AgressoId er upresis, men orgenhet på ressurs er presis")
    void test_UpresisAgressoID_PresisOrgenhetPaaRessurs_IderErMatch(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID", "Enhet_1", "AGRID_1", "ORGENHET", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), Optional.empty()),
            dvhRessursStilling(A000001, "AGRID", "Enhet_1", "AGRID_1", "ORGENHET", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), Optional.empty()),
            dvhRessursStilling(A000001, "AGRID", "Enhet_1", "AGRID_1", "ORGENHET", LocalDate.of(2022, 1, 1), MAX_DATE, Optional.empty())
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 1.vi
    @DisplayName("AgressoId presis, men orgenhet på ressurs er upresis - Id'ene matcher ikke")
    void test_PresisAgressoID_UpresisOrgenhetPaaRessurs_IderMatcherIkke(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AID_2", "ORGENHET", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), Optional.empty()),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AID_2", "ORGENHET", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), Optional.empty()),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AID_2", "ORGENHET", LocalDate.of(2022, 1, 1), MAX_DATE, Optional.empty())
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("AID_2", forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 1.vii
    @DisplayName("Både AgressoId og orgenhet på ressurs er upresis - Id'ene matcher")
    void test_UpresisAgressoID_PresisOrgenhetPaaRessurs_IderMatcher(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_1", "ORGNIV3", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), Optional.empty()),
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_1", "ORGNIV3", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), Optional.empty()),
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_1", "ORGNIV3", LocalDate.of(2022, 1, 1), MAX_DATE, Optional.empty())
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AID_100", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("AID_1", forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGNIV3", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 1.viii
    @DisplayName("Både AgressoId og orgenhet på ressurs er upresis - Id'ene matcher ikke")
    void test_UpresisAgressoID_UpresisOrgenhetPaaRessurs_IderMatcherIkke(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_2", "ORGNIV3", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), Optional.empty()),
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_2", "ORGNIV3", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), Optional.empty()),
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_2", "ORGNIV3", LocalDate.of(2022, 1, 1), MAX_DATE, Optional.empty())
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AID_100", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("AID_2", forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGNIV3", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 1.ix
    @DisplayName("AgressoId mangler, men orgenhet på ressurs er presis")
    void test_ManglerAgressoID_PresisOrgenhetPaaRessurs(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "-4", "Mangler", "AGRID_1", "ORGENHET", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), Optional.empty()),
            dvhRessursStilling(A000001, "-4", "Mangler", "AGRID_1", "ORGENHET", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), Optional.empty()),
            dvhRessursStilling(A000001, "-4", "Mangler", "AGRID_1", "ORGENHET", LocalDate.of(2022, 1, 1), MAX_DATE, Optional.empty())
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("-4", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Mangler", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 1.x
    @DisplayName("AgressoId mangler, og orgenhet på ressurs er upresis")
    void test_ManglerAgressoID_UpresisOrgenhetPaaRessurs(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "-4", "Mangler", "AID_1", "ORGNIV3", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), Optional.empty()),
            dvhRessursStilling(A000001, "-4", "Mangler", "AID_1", "ORGNIV3", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), Optional.empty()),
            dvhRessursStilling(A000001, "-4", "Mangler", "AID_1", "ORGNIV3", LocalDate.of(2022, 1, 1), MAX_DATE, Optional.empty())
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("-4", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("AID_1", forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Mangler", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGNIV3", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 1.xi
    @DisplayName("Mangler både AgressoId og orgenhet på ressurs")
    void test_ManglerAgressoID_ManglerOrgenhetPaaRessurs(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "-4", "Mangler", "-3", null, LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31), Optional.empty()),
            dvhRessursStilling(A000001, "-4", "Mangler", "-3", null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), Optional.empty()),
            dvhRessursStilling(A000001, "-4", "Mangler", "-3", null, LocalDate.of(2022, 1, 1), MAX_DATE, Optional.empty())
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("-4", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("-3", forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Mangler", forsteStillingPeriode.stilling().enhetNavn());
        assertNull(forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 2.i
    @DisplayName("Perioder henger sammen, og første periode slutter før ressursens startdato")
    void test_FoerstePeriodeSlutterFoerRessursensStartDato(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2020, 11, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 11, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());

        assertEquals(1, result.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getFirst().errorType());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 2.ii
    @DisplayName("Ulike perioder hvor første periode starter før ressursen startdato, men slutter etter")
    void test_FoerstePeriodeStarterFoerRessursensStartDato(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2020, 5, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 5, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());

        assertEquals(1, result.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getFirst().errorType());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 2.iii
    @DisplayName("Ulike perioder henger sammen, og første periode slutter før ressursens startdato, den andre starter før ressursens startdato")
    void test_UlikePerioder_FoersteMedPeriodeFoerOgEnSomStarterFoerRessursensStartDato(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2021, 11, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2021, 11, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2021, 12, 31), forsteStillingPeriode.gyldigTom());

        assertEquals("AGRID_2", andreStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_2", andreStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", andreStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2022, 1, 1), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, andreStillingPeriode.gyldigTom());

        assertEquals(2, result.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getFirst().errorType());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getLast().errorType());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 3.i
    @DisplayName("Perioder henger sammen, og siste periode slutter etter ressursens sluttdato")
    void test_SistePeriodeSlutterEtterRessursensStartDato(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2019, 1, 1), LocalDate.of(2023, 12, 31))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2023, 12, 31), forsteStillingPeriode.gyldigTom());

        assertEquals(1, result.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getFirst().errorType());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 3.ii
    @DisplayName("Perioder henger sammen, og siste periode starter etter ressursens sluttdato")
    void test_SistePeriodeStarterEtterRessursensStartDato(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2019, 1, 1), LocalDate.of(2021, 12, 31))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2021, 12, 31), forsteStillingPeriode.gyldigTom());

        assertEquals(1, result.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getFirst().errorType());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 3.iii
    @DisplayName("Ulike perioder henger sammen, den ene med periode som slutter etter ressursen sluttdato, den andre med periode etter ressursen sluttdato")
    void test_UlikePerioder_FoersteMedPeriodeSlutterEtterRessursensSluttDato_AndreStarterEtterRessursenSluttdato(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2019, 1, 1), LocalDate.of(2021, 8, 31))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2021, 8, 31), forsteStillingPeriode.gyldigTom());

        assertEquals(2, result.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getFirst().errorType());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, result.errors().getLast().errorType());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 4.i
    @DisplayName("Like enheter, men overlapp av periode")
    void test_OverlappAvPeriode(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 3, 1), MAX_DATE)
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        var forsteStillingPeriode = result.stillingPerioder().getFirst();

        assertEquals(1, result.stillingPerioder().size());

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());

        assertEquals(1, result.errors().size());
        assertEquals(DVH_RESSURSSTILLINGER_HAR_OVERLAPPENDE_PERIODER, result.errors().getFirst().errorType());
    }


    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 4.ii
    @DisplayName("Splitt på ulike enheter med overlapp av periode - Flere overlapp")
    void test_SplittPaaUlikeEnheterMedOverlapp_FlereOverlapp(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 3, 1), MAX_DATE), // Overlapper den over
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), MAX_DATE), // Overlapper den under
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31))
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();

        assertEquals(2, result.stillingPerioder().size());

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());

        assertEquals("AGRID_2", andreStillingPeriode.stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 11, 1), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, andreStillingPeriode.gyldigTom());

        assertEquals(2, result.errors().size());
        assertEquals(DVH_RESSURSSTILLINGER_HAR_OVERLAPPENDE_PERIODER, result.errors().getFirst().errorType());
        assertEquals(DVH_RESSURSSTILLINGER_HAR_OVERLAPPENDE_PERIODER, result.errors().getFirst().errorType());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 5
    @DisplayName("Splitt på ulike sektor og tilknytninger, men sammenhengende datoer")
    void test_splittPaaUlikeSektorMedSammenhengendeDatoer(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), Optional.of("Midlertidig karakter § 9a")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 6, 15), Optional.of("Midlertidig karakter § 9a")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 6, 16), MAX_DATE, Optional.of("Fast"))
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2021, 6, 15), forsteStillingPeriode.gyldigTom());
        assertEquals(Sektor.NAV_STATLIG, forsteStillingPeriode.sektor());
        assertEquals(Tilknytningtype.MIDLERTIDIG, forsteStillingPeriode.tilknytningtype().orElse(null));
        var andreStillingPeriode = result.stillingPerioder().getLast();
        assertEquals("AGRID_1", andreStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1", andreStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", andreStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2021, 6, 16), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, andreStillingPeriode.gyldigTom());
        assertEquals(Sektor.NAV_STATLIG, andreStillingPeriode.sektor());
        assertEquals(Tilknytningtype.FAST, andreStillingPeriode.tilknytningtype().orElse(null));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    // Test referanser 6
    @DisplayName("Slå sammen sammenhengende perioder med like sektor og tilknytninger")
    void test_SlaaSammenSammenhengendePerioderMedLikeSektorOgTilknytninger(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 6, 15), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 6, 16), MAX_DATE, Optional.of("Fast"))
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());
        assertEquals(Sektor.NAV_STATLIG, forsteStillingPeriode.sektor());
        assertEquals(Tilknytningtype.FAST, forsteStillingPeriode.tilknytningtype().orElse(null));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Ikke slå sammen sammenhegende perioder med ulike sektor og tilknytninger")
    void test_IkkeSlaaSammenSammenhengendePerioderMedUlikeSektorOgTilknytninger(boolean unleashBoolean) {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), Optional.of("Midlertidig karakter § 9a")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 6, 15), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 6, 16), MAX_DATE, Optional.of("Midlertidig karakter § 9a"))
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(unleashBoolean);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(3, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().get(1);
        var tredjeStillingPeriode = result.stillingPerioder().getLast();

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2020, 12, 31), forsteStillingPeriode.gyldigTom());
        assertEquals(Sektor.NAV_STATLIG, forsteStillingPeriode.sektor());
        assertEquals(Tilknytningtype.MIDLERTIDIG, forsteStillingPeriode.tilknytningtype().orElse(null));
        assertEquals("AGRID_1", andreStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1", andreStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", andreStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2021, 1, 1), andreStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2021, 6, 15), andreStillingPeriode.gyldigTom());
        assertEquals(Sektor.NAV_STATLIG, andreStillingPeriode.sektor());
        assertEquals(Tilknytningtype.FAST, andreStillingPeriode.tilknytningtype().orElse(null));
        assertEquals("AGRID_1", tredjeStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1", tredjeStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", tredjeStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2021, 6, 16), tredjeStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, tredjeStillingPeriode.gyldigTom());
        assertEquals(Sektor.NAV_STATLIG, tredjeStillingPeriode.sektor());
        assertEquals(Tilknytningtype.MIDLERTIDIG, tredjeStillingPeriode.tilknytningtype().orElse(null));
    }

    @Test
    @DisplayName("Opprett stillingperioder med lik dato, men ulike mappede tilknytningtyper")
    void test_StillingPerioderMedBareMappedeTilknytningtyperOgLikPeriode() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), Optional.of("Midlertidig karakter § 9a")),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), Optional.of("Utdanningsstillinger § 9d"))
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(4, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().get(1);
        var tredjeStillingPeriode = result.stillingPerioder().get(2);
        var fjerdeStillingPeriode = result.stillingPerioder().getLast();

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals(Optional.of(Tilknytningtype.FAST), forsteStillingPeriode.tilknytningtype());
        assertEquals("AGRID_1", andreStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals(Optional.of(Tilknytningtype.MIDLERTIDIG), andreStillingPeriode.tilknytningtype());
        assertEquals("AGRID_2", tredjeStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals(Optional.of(Tilknytningtype.FAST), tredjeStillingPeriode.tilknytningtype());
        assertEquals("AGRID_2", fjerdeStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals(Optional.of(Tilknytningtype.UTDANNINGSSTILLINGER), fjerdeStillingPeriode.tilknytningtype());
    }

    @Test
    @DisplayName("Opprett stillingperioder med lik dato, men med mappede og ikke mappede tilknytningtyper")
    void test_StillingPerioderMedMappedeOgIkkeMappedeTilknytningtyperMedLikPeriode() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), Optional.of("Uforutsett behov § 9e")),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), Optional.of("Timelønn"))
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals(Optional.of(Tilknytningtype.FAST), forsteStillingPeriode.tilknytningtype());
        assertEquals("AGRID_2", andreStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals(Optional.of(Tilknytningtype.UFORUTSETT_BEHOV), andreStillingPeriode.tilknytningtype());
    }

    @Test
    @DisplayName("Opprett stillingperioder hvor en periode dekker hele andre perioden, hvor den større perioden har tilknytningstype")
    void test_StillingPeriodeMedTilknytningPaaDenSomHarLengstPeriode() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), Optional.of("Timelønn"))
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals(Optional.of(Tilknytningtype.FAST), forsteStillingPeriode.tilknytningtype());
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 12, 31), forsteStillingPeriode.gyldigTom());
    }

    @Test
    @DisplayName("Opprett stillingperioder hvor en periode dekker hele andre perioden, hvor den mindre perioden har tilknytningstype")
    void test_StillingPeriodeMedTilknytningPaaDenSomHarKortestPeriode() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), Optional.of("Timelønn")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), Optional.of("Timelønn")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31), Optional.of("Timelønn")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), Optional.of("Fast"))
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(3, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().get(1);
        var tredjeStillingPeriode = result.stillingPerioder().getLast();

        assertAll(
            () -> assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse(null)),
            () -> assertEquals(Optional.empty(), forsteStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2020, 12, 31), forsteStillingPeriode.gyldigTom()),
            () -> assertEquals("AGRID_1", andreStillingPeriode.stilling().agressoId().orElse(null)),
            () -> assertEquals(Optional.of(Tilknytningtype.FAST), andreStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2021, 1, 1), andreStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2021, 12, 31), andreStillingPeriode.gyldigTom()),
            () -> assertEquals("AGRID_1", tredjeStillingPeriode.stilling().agressoId().orElse(null)),
            () -> assertEquals(Optional.empty(), tredjeStillingPeriode.tilknytningtype()),
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
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 7, 1), LocalDate.of(2021, 6, 30), Optional.of("Fast"))
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(3, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().get(1);
        var tredjeStillingPeriode = result.stillingPerioder().getLast();

        assertAll(
            () -> assertEquals(Optional.empty(), forsteStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2020, 6, 30), forsteStillingPeriode.gyldigTom()),
            () -> assertEquals(Optional.of(Tilknytningtype.FAST), andreStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2020, 7, 1), andreStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2021, 6, 30), andreStillingPeriode.gyldigTom()),
            () -> assertEquals(Optional.empty(), tredjeStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2021, 7, 1), tredjeStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2021, 12, 31), tredjeStillingPeriode.gyldigTom())
        );
    }

    @Test
    @DisplayName("Tre stillingperioder uten tilknytning som starter før og slutter etter to stillingperioder med tilknytning")
    void test_TreStillingPerioderUtenTilknytningSomStarterFoerOgSlutterEtterToStillingPerioderMedTilknytning() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 7, 1), LocalDate.of(2021, 5, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2021, 7, 1), LocalDate.of(2022, 6, 30), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 10, 31))
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);
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
            () -> assertEquals(Optional.empty(), forsteStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2020, 6, 30), forsteStillingPeriode.gyldigTom()),
            () -> assertEquals(Optional.of(Tilknytningtype.FAST), andreStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2020, 7, 1), andreStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2021, 5, 31), andreStillingPeriode.gyldigTom()),
            () -> assertEquals(Optional.empty(), tredjeStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2021, 6, 1), tredjeStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2021, 6, 30), tredjeStillingPeriode.gyldigTom()),
            () -> assertEquals(Optional.of(Tilknytningtype.FAST), fjerdeStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2021, 7, 1), fjerdeStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2022, 6, 30), fjerdeStillingPeriode.gyldigTom()),
            () -> assertEquals(Optional.empty(), femteStillingPeriode.tilknytningtype()),
            () -> assertEquals(LocalDate.of(2022, 7, 1), femteStillingPeriode.gyldigFom()),
            () -> assertEquals(LocalDate.of(2022, 10, 31), femteStillingPeriode.gyldigTom())
        );
    }

    @Test
    @DisplayName("Får null i perioderGruppert")
    void test_FaarNullIPerioderGruppert() {
        List<DvhRessursStilling> dvhRessursStillinger = Collections.emptyList();

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);
        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(result.stillingPerioder(), Collections.emptyList());
    }

    @Test
    @DisplayName("Test case fra DEV Miljø hvor ressursen ender opp med to like OTer, men forskjellige perioder")
    void test_ToLikeOTerMedDelvisOverlappendePerioder() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 4, 1), LocalDate.of(2022, 4, 30), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 5, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 6, 1), LocalDate.of(2022, 6, 1), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 6, 2), LocalDate.of(2022, 7, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 8), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 8, 9), LocalDate.of(2022, 9, 30), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 10, 1), LocalDate.of(2022, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 4, 30), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 5, 1), LocalDate.of(2023, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 4, 1), LocalDate.of(2024, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 5, 1), LocalDate.of(2024, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 6, 1), LocalDate.of(2024, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 9, 1), LocalDate.of(2024, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 11, 1), LocalDate.of(2024, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 2, 1), LocalDate.of(2025, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 5, 1), LocalDate.of(2025, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2026, 1, 1), LocalDate.of(2099, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 4, 1), LocalDate.of(2022, 4, 30), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 5, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 6, 1), LocalDate.of(2022, 6, 1), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 6, 2), LocalDate.of(2022, 7, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 8), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 8, 9), LocalDate.of(2022, 9, 30), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 10, 1), LocalDate.of(2022, 12, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 4, 30), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 5, 1), LocalDate.of(2023, 12, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 6, 1), LocalDate.of(2024, 8, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 9, 1), LocalDate.of(2024, 10, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 11, 1), LocalDate.of(2024, 12, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 4, 30), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 5, 1), LocalDate.of(2025, 12, 31), Optional.of("Innlån/utlån"))
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);
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
            () -> assertEquals(stillingPeriode1.tilknytningtype(), Optional.of(Tilknytningtype.FAST)),
            () -> assertEquals(stillingPeriode2.gyldigFom(), LocalDate.of(2022, 6, 1)),
            () -> assertEquals(stillingPeriode2.gyldigTom(), LocalDate.of(2022, 7, 31)),
            () -> assertEquals(stillingPeriode2.tilknytningtype(), Optional.of(Tilknytningtype.FAST)),
            () -> assertEquals(stillingPeriode3.gyldigFom(), LocalDate.of(2022, 8, 1)),
            () -> assertEquals(stillingPeriode3.gyldigTom(), LocalDate.of(2099, 12, 31)),
            () -> assertEquals(stillingPeriode3.tilknytningtype(), Optional.of(Tilknytningtype.FAST))
        );
    }

    @Test
    @DisplayName("Test case with provided data")
    void test_ProvidedData() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 4, 1), LocalDate.of(2022, 4, 30), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 4, 1), LocalDate.of(2022, 4, 30), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 12, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 4, 30), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 4, 30), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 5, 1), LocalDate.of(2023, 11, 30), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 5, 1), LocalDate.of(2023, 11, 30), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 12, 1), LocalDate.of(2023, 12, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2023, 12, 1), LocalDate.of(2023, 12, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 5, 1), LocalDate.of(2024, 7, 31), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 5, 1), LocalDate.of(2024, 7, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 14), Optional.of("Innlån/utlån")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 14), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 8, 15), LocalDate.of(2024, 8, 31), Optional.of("Fungering")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 8, 15), LocalDate.of(2024, 8, 31), Optional.of("Fungering")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 9, 1), LocalDate.of(2024, 9, 30), Optional.of("Fungering")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 9, 1), LocalDate.of(2024, 9, 30), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 31), Optional.of("Fungering")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 11, 1), LocalDate.of(2024, 11, 30), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 11, 1), LocalDate.of(2024, 11, 30), Optional.of("Fungering")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 12, 1), LocalDate.of(2025, 1, 31), Optional.of("Fungering")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2024, 12, 1), LocalDate.of(2025, 1, 31), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 2, 1), LocalDate.of(2025, 4, 30), Optional.of("Fungering")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 2, 1), LocalDate.of(2025, 4, 30), Optional.of("Fast")),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2025, 5, 1), LocalDate.of(2099, 12, 31), Optional.of("Fast"))
        );

        when(unleash.isEnabled("nom.ny-tilknytning-mapping")).thenReturn(true);
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
            () -> assertEquals(stillingPeriode1.tilknytningtype(), Optional.of(Tilknytningtype.FAST)),
            () -> assertEquals(stillingPeriode2.gyldigFom(), LocalDate.of(2024, 8, 15)),
            () -> assertEquals(stillingPeriode2.gyldigTom(), LocalDate.of(2024, 8, 31)),
            () -> assertEquals(stillingPeriode2.tilknytningtype(), Optional.empty()),
            () -> assertEquals(stillingPeriode3.gyldigFom(), LocalDate.of(2024, 9, 1)),
            () -> assertEquals(stillingPeriode3.gyldigTom(), LocalDate.of(2099, 12, 31)),
            () -> assertEquals(stillingPeriode3.tilknytningtype(), Optional.of(Tilknytningtype.FAST))
        );
    }
}