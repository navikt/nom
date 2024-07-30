package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.DVH_RESSURSSTILLINGER_HAR_OVERLAPPENDE_PERIODER;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.NAVIDENT_FINNES_IKKE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

// REF: no/nav/nom/apps/api/orgtilknytning/dvh/dokumentasjon/TestReferanser_GrupperOgSlaaSammenPerioder.md
@DisplayName("Tester gruppering og strukturering av periodisering på RessursStillinger fra DVH")
class NyeRessursStillingStrukturererGrupperOgSlaaSammenTest extends NyeRessursStillingStrukturererBasis {

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
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        assertEquals(1, strukturerteStillingPerioder.stillingPerioder().size());
        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        assertEquals("AGRID_1",  forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1",  forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                 forsteStillingPeriode.gyldigTom());
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
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        var andreStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getLast();

        assertEquals(2, strukturerteStillingPerioder.stillingPerioder().size());

        assertEquals("AGRID_1",  forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1",  forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));

        assertEquals("AGRID_1",  andreStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1",  andreStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", andreStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));

        assertEquals(LocalDate.of(2020, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2020, 8, 31), forsteStillingPeriode.gyldigTom());

        assertEquals(LocalDate.of(2020, 11, 1), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                  andreStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.ii
    @DisplayName("Splitt på ulike enheter")
    public void test_SplittPaaUlikeEnheter() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", "AGRID_x", "ORGENHET", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_2", "Enhet_2", "AGRID_x", "ORGENHET", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));

        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        var andreStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getLast();

        assertEquals(2, strukturerteStillingPerioder.stillingPerioder().size());

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2020, 10, 31), forsteStillingPeriode.gyldigTom());

        assertEquals("AGRID_2", andreStillingPeriode.stilling().agressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 11, 1), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, andreStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.iii
    @DisplayName("Splitt på ulike 'OrgenhetPåRessurs' selv om 'AgressoId' er lik")
    public void test_SplittPaaUlikeOrgenhetPaaRessurs() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AGRID_x", "ORGENHET", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AGRID_x", "ORGENHET", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));

        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        var andreStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getLast();

        assertEquals(2, strukturerteStillingPerioder.stillingPerioder().size());

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().agressoId().orElse("NULL"));
        assertEquals("AGRID_1", forsteStillingPeriode.stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2020, 10, 31), forsteStillingPeriode.gyldigTom());

        assertEquals("AGRID_1", andreStillingPeriode.stilling().agressoId().orElse("NULL"));
        assertEquals("AGRID_x", andreStillingPeriode.stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 11, 1), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, andreStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.iv
    @DisplayName("AgressoId presis, men orgenhet på ressurs er upresis")
    void test_PresisAgressoID_UpresisOrgenhetPaaRessurs_IderErMatch() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AGRID", "ORGNIV3", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AGRID", "ORGNIV3", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AGRID", "ORGNIV3", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));

        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        assertEquals(1, strukturerteStillingPerioder.stillingPerioder().size());
        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        assertEquals("AGRID_1",  forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("AGRID",    forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Enhet_1",  forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGNIV3", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                 forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.v
    @DisplayName("AgressoId er upresis, men orgenhet på ressurs er presis")
    void test_UpresisAgressoID_PresisOrgenhetPaaRessurs_IderErMatch() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID", "Enhet_1", "AGRID_1", "ORGENHET", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID", "Enhet_1", "AGRID_1", "ORGENHET", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID", "Enhet_1", "AGRID_1", "ORGENHET", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));

        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        assertEquals(1, strukturerteStillingPerioder.stillingPerioder().size());
        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        assertEquals("AGRID",    forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("AGRID_1",  forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Enhet_1",  forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                 forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.vi
    @DisplayName("AgressoId presis, men orgenhet på ressurs er upresis - Id'ene matcher ikke")
    void test_PresisAgressoID_UpresisOrgenhetPaaRessurs_IderMatcherIkke() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AID_2", "ORGENHET", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AID_2", "ORGENHET", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", "AID_2", "ORGENHET", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));

        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        assertEquals(1, strukturerteStillingPerioder.stillingPerioder().size());
        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        assertEquals("AGRID_1",  forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("AID_2",    forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Enhet_1",  forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                 forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.vii
    @DisplayName("Både AgressoId og orgenhet på ressurs er upresis - Id'ene matcher")
    void test_UpresisAgressoID_PresisOrgenhetPaaRessurs_IderMatcher() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_1", "ORGNIV3", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_1", "ORGNIV3", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_1", "ORGNIV3", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));

        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        assertEquals(1, strukturerteStillingPerioder.stillingPerioder().size());
        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        assertEquals("AID_100",  forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("AID_1",   forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGNIV3", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                 forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.viii
    @DisplayName("Både AgressoId og orgenhet på ressurs er upresis - Id'ene matcher ikke")
    void test_UpresisAgressoID_UpresisOrgenhetPaaRessurs_IderMatcherIkke() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_2", "ORGNIV3", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_2", "ORGNIV3", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AID_100", "Enhet_1", "AID_2", "ORGNIV3", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));

        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        assertEquals(1, strukturerteStillingPerioder.stillingPerioder().size());
        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        assertEquals("AID_100",  forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("AID_2",   forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Enhet_1", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGNIV3", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                 forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.ix
    @DisplayName("AgressoId mangler, men orgenhet på ressurs er presis")
    void test_ManglerAgressoID_PresisOrgenhetPaaRessurs() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "-4", "Mangler", "AGRID_1", "ORGENHET", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "-4", "Mangler", "AGRID_1", "ORGENHET", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "-4", "Mangler", "AGRID_1", "ORGENHET", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));

        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        assertEquals(1, strukturerteStillingPerioder.stillingPerioder().size());
        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        assertEquals("-4",       forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("AGRID_1",  forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Mangler",  forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                 forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.x
    @DisplayName("AgressoId mangler, og orgenhet på ressurs er upresis")
    void test_ManglerAgressoID_UpresisOrgenhetPaaRessurs() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "-4", "Mangler", "AID_1", "ORGNIV3", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "-4", "Mangler", "AID_1", "ORGNIV3", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "-4", "Mangler", "AID_1", "ORGNIV3", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));

        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        assertEquals(1, strukturerteStillingPerioder.stillingPerioder().size());
        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        assertEquals("-4",      forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("AID_1",   forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Mangler", forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGNIV3", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                 forsteStillingPeriode.gyldigTom());
    }

    @Test
    // Test referanser 1.xi
    @DisplayName("Mangler både AgressoId og orgenhet på ressurs")
    void test_ManglerAgressoID_ManglerOrgenhetPaaRessurs() {
        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "-4", "Mangler", "-3", null, LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "-4", "Mangler", "-3", null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "-4", "Mangler", "-3", null, LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(ressursStateRepository.findByNavident(A000001)).thenReturn(Optional.of(ressurs(A000001, LocalDate.of(2010, 1, 1))));

        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, dvhRessursStillinger);
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        assertEquals(1, strukturerteStillingPerioder.stillingPerioder().size());
        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        assertEquals("-4",      forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("-3",      forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Mangler", forsteStillingPeriode.stilling().enhetNavn());
        assertNull(forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                 forsteStillingPeriode.gyldigTom());
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
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        assertEquals(1, strukturerteStillingPerioder.stillingPerioder().size());
        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        assertEquals("AGRID_1",  forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1",  forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 11, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                  forsteStillingPeriode.gyldigTom());

        assertEquals(1, strukturerteStillingPerioder.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, strukturerteStillingPerioder.errors().getFirst().errorType());
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
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        assertEquals(1, strukturerteStillingPerioder.stillingPerioder().size());
        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        assertEquals("AGRID_1",  forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1",  forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 5, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                 forsteStillingPeriode.gyldigTom());

        assertEquals(1, strukturerteStillingPerioder.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, strukturerteStillingPerioder.errors().getFirst().errorType());
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
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        assertEquals(2, strukturerteStillingPerioder.stillingPerioder().size());
        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        var andreStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getLast();

        assertEquals("AGRID_1",  forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1",  forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2021, 11, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2021, 12, 31), forsteStillingPeriode.gyldigTom());

        assertEquals("AGRID_2",  andreStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_2",  andreStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", andreStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2022, 1, 1), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                 andreStillingPeriode.gyldigTom());

        assertEquals(2, strukturerteStillingPerioder.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, strukturerteStillingPerioder.errors().getFirst().errorType());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, strukturerteStillingPerioder.errors().getLast().errorType());
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
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        assertEquals(1, strukturerteStillingPerioder.stillingPerioder().size());
        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        assertEquals("AGRID_1",  forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1",  forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1),   forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2023, 12, 31), forsteStillingPeriode.gyldigTom());

        assertEquals(1, strukturerteStillingPerioder.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, strukturerteStillingPerioder.errors().getFirst().errorType());
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
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        assertEquals(1, strukturerteStillingPerioder.stillingPerioder().size());
        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        assertEquals("AGRID_1",  forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1",  forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2021, 12, 31), forsteStillingPeriode.gyldigTom());

        assertEquals(1, strukturerteStillingPerioder.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, strukturerteStillingPerioder.errors().getFirst().errorType());
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
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        assertEquals(1, strukturerteStillingPerioder.stillingPerioder().size());
        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        assertEquals("AGRID_1",  forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Enhet_1",  forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET", forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2021, 8, 31), forsteStillingPeriode.gyldigTom());

        assertEquals(2, strukturerteStillingPerioder.errors().size());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, strukturerteStillingPerioder.errors().getFirst().errorType());
        assertEquals(DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET, strukturerteStillingPerioder.errors().getLast().errorType());
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
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();

        assertEquals(1, strukturerteStillingPerioder.stillingPerioder().size());

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());

        assertEquals(1, strukturerteStillingPerioder.errors().size());
        assertEquals(DVH_RESSURSSTILLINGER_HAR_OVERLAPPENDE_PERIODER, strukturerteStillingPerioder.errors().getFirst().errorType());
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
        CUT.grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, strukturerteStillingPerioder);

        var forsteStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getFirst();
        var andreStillingPeriode = strukturerteStillingPerioder.stillingPerioder().getLast();

        assertEquals(2, strukturerteStillingPerioder.stillingPerioder().size());

        assertEquals("AGRID_1", forsteStillingPeriode.stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 1, 1), forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, forsteStillingPeriode.gyldigTom());

        assertEquals("AGRID_2",andreStillingPeriode.stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 11, 1), andreStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE, andreStillingPeriode.gyldigTom());

        assertEquals(2, strukturerteStillingPerioder.errors().size());
        assertEquals(DVH_RESSURSSTILLINGER_HAR_OVERLAPPENDE_PERIODER, strukturerteStillingPerioder.errors().getFirst().errorType());
        assertEquals(DVH_RESSURSSTILLINGER_HAR_OVERLAPPENDE_PERIODER, strukturerteStillingPerioder.errors().getFirst().errorType());
    }
}