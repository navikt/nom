package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode.DagligOppfolging.JA;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode.DagligOppfolging.NEI;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode.DagligOppfolging.UKJENT;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.NAVIDENT_FINNES_IKKE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("Tester gruppering og strukturering av periodisering på RessursStillinger fra DVH")
class NyeRessursStillingStrukturererTest extends NyeRessursStillingStrukturererBasis {

    @Test
    @DisplayName("Validate error on navident from DVH missing in NOM")
    void test_missingNavidentInNom() {
        System.out.printf("""
                          DVH RessursStilling for Navident %s starter (%s) på eller etter ressursens sluttdato %s.
                          Behandler ikke denne eller etterfølgende DVH RessursStillinger for denne navidenten.
                          %n""", "UNKNOWN", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31));
        List<DvhRessursStilling> dvhResssursStillinger = List.of(
            dvhRessursStilling("A000001", "STILLING1", "ORGENHET1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling("UNKNOWN", "STILLING2", "ORGENHET2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
        );

        when(ressursStateRepository.findByNavident("A000001")).thenReturn(Optional.of(ressurs("A000001")));
        when(ressursStateRepository.findByNavident("UNKNOWN")).thenReturn(Optional.empty());

        var strukturerteStillingPerioder = CUT.strukturerStilingPerioderPerNavident(dvhResssursStillinger);

        assertEquals(2, strukturerteStillingPerioder.size());
        assertFalse(strukturerteStillingPerioder.get("A000001").dvhRessursStillinger().isEmpty());
        assertTrue(strukturerteStillingPerioder.get("UNKNOWN").stillingPerioder().isEmpty());
        assertEquals(1, strukturerteStillingPerioder.get("UNKNOWN").errors().size());
        assertEquals(NAVIDENT_FINNES_IKKE, strukturerteStillingPerioder.get("UNKNOWN").errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Test Gruppering per navident av RessursStillinger")
    void test_GrupperPerNavident() {
        String A000001 = "A000001";
        String B000001 = "B000001";

        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE),
            dvhRessursStilling(B000001, "AGRID_2", "Enhet_2", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(B000001, "AGRID_2", "Enhet_2", LocalDate.of(2020, 1, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(B000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        var result = CUT.grupperDvhRessursStillingerPerNavident(dvhRessursStillinger);

        assertEquals(2, result.size());
        assertEquals(3, result.get(A000001).size());
        assertEquals(3, result.get(B000001).size());
    }


    @Test
    @DisplayName("Test Gruppering per navident av RessursStillinger - annen sortering")
    void test_GrupperPerNavident_NySortering() {
        String A000001 = "A000001";
        String B000001 = "B000001";

        List<DvhRessursStilling> dvhRessursStillinger = List.of(
            dvhRessursStilling(B000001, "AGRID_2", "Enhet_2", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(B000001, "AGRID_2", "Enhet_2", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE),
            dvhRessursStilling(A000001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            dvhRessursStilling(B000001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        var result = CUT.grupperDvhRessursStillingerPerNavident(dvhRessursStillinger);

        assertEquals(2, result.size());
        assertEquals(3, result.get(A000001).size());
        assertEquals(3, result.get(B000001).size());
    }

    @Test
    @DisplayName("Test for sammenhengende Stillingperioder i annen StillingPeriodes periode - single eksisterende dekker eksakt")
    void dekkerEksisterendeStillingPerioderAngittStillingPeriode_singleEksaktOverlapp() {
        List<StillingPeriode> eksisterendeStillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), null)
        );

        var aktuellStillingPeriode = new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), null);
        var result = CUT.dekkerEksisterendeStillingPerioderAngittStillingPeriode(eksisterendeStillingPerioder, aktuellStillingPeriode);

        assertTrue(result);
    }

    @Test
    @DisplayName("Test for sammenhengende Stillingperioder i annen StillingPeriodes periode - single eksisterende dekker langt utover")
    void dekkerEksisterendeStillingPerioderAngittStillingPeriode_singleOverlappLangtUtover() {
        List<StillingPeriode> eksisterendeStillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2021, 12, 31), null)
        );

        var aktuellStillingPeriode = new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), null);
        var result = CUT.dekkerEksisterendeStillingPerioderAngittStillingPeriode(eksisterendeStillingPerioder, aktuellStillingPeriode);

        assertTrue(result);
    }

    @Test
    @DisplayName("Test for sammenhengende Stillingperioder i annen StillingPeriodes periode - en single eksisterende dekker eksakt, flere andre med og uten overlapp inn i perioden")
    void dekkerEksisterendeStillingPerioderAngittStillingPeriode_singleEksaktOverlapp_FlereAndreMedOverlapp() {
        List<StillingPeriode> eksisterendeStillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), null), // Før
            new StillingPeriode(null, LocalDate.of(2019, 5, 1), LocalDate.of(2020,  1, 31), null), // Overlapp inn i periode
            new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), null), // Dekker eksakt
            new StillingPeriode(null, LocalDate.of(2020, 9, 1), LocalDate.of(2021, 12, 31), null), // Overlapper på slutten
            new StillingPeriode(null, LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31), null) // Kommer etter
        );

        var aktuellStillingPeriode = new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), null);
        var result = CUT.dekkerEksisterendeStillingPerioderAngittStillingPeriode(eksisterendeStillingPerioder, aktuellStillingPeriode);

        assertTrue(result);
    }

    @Test
    @DisplayName("Test for sammenhengende Stillingperioder i annen StillingPeriodes periode - flere eksisterende dekker med back to back, flere andre med og uten overlapp inn i perioden")
    void dekkerEksisterendeStillingPerioderAngittStillingPeriode_FlereBackToBackLangtUtover_FlereAndreMedOverlapp() {
        List<StillingPeriode> eksisterendeStillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), null), // Før
            new StillingPeriode(null, LocalDate.of(2019, 5, 1), LocalDate.of(2020, 1, 31), null), // Overlapp inn i periode
            // Disse tre dekker tilsammen, med litt overlapp
            new StillingPeriode(null, LocalDate.of(2029, 5, 1), LocalDate.of(2020, 1, 31), null), // Dekker
            new StillingPeriode(null, LocalDate.of(2020, 2, 1), LocalDate.of(2020, 4, 30), null), // Dekker
            new StillingPeriode(null, LocalDate.of(2020, 5, 1), LocalDate.of(2021, 2, 28), null), // Dekker

            new StillingPeriode(null, LocalDate.of(2020, 9, 1), LocalDate.of(2021, 12, 31), null), // Overlapper på slutten
            new StillingPeriode(null, LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31), null) // Kommer etter
        );

        var aktuellStillingPeriode = new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), null);
        var result = CUT.dekkerEksisterendeStillingPerioderAngittStillingPeriode(eksisterendeStillingPerioder, aktuellStillingPeriode);

        assertTrue(result);
    }

    @Test
    @DisplayName("Test for sammenhengende Stillingperioder i annen StillingPeriodes periode - flere eksisterende dekker med overlapp, flere andre med og uten overlapp inn i perioden")
    void dekkerEksisterendeStillingPerioderAngittStillingPeriode_FlereOverlappLangtUtover_FlereAndreMedOverlapp() {
        List<StillingPeriode> eksisterendeStillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), null), // Før
            new StillingPeriode(null, LocalDate.of(2019, 5, 1), LocalDate.of(2020,  1, 31), null), // Overlapp inn i periode
            // Disse tre dekker tilsammen, med litt overlapp
            new StillingPeriode(null, LocalDate.of(2019, 5, 1), LocalDate.of(2020,  1, 31), null), // Dekker
            new StillingPeriode(null, LocalDate.of(2020, 2, 1), LocalDate.of(2020,  5, 15), null), // Dekker
            new StillingPeriode(null, LocalDate.of(2020, 5, 1), LocalDate.of(2021,  2, 28), null), // Dekker

            new StillingPeriode(null, LocalDate.of(2020, 9, 1), LocalDate.of(2021, 12, 31), null), // Overlapper på slutten
            new StillingPeriode(null, LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31), null) // Kommer etter
        );

        var aktuellStillingPeriode = new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), null);
        var result = CUT.dekkerEksisterendeStillingPerioderAngittStillingPeriode(eksisterendeStillingPerioder, aktuellStillingPeriode);

        assertTrue(result);
    }

    @Test
    @DisplayName("Test for sammenhengende Stillingperioder i annen StillingPeriodes periode - flere eksisterende med hull, flere andre med og uten overlapp inn i perioden")
    void dekkerEksisterendeStillingPerioderAngittStillingPeriode_FlereMedHull_FlereAndreMedOverlapp() {
        List<StillingPeriode> eksisterendeStillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), null), // Før
            new StillingPeriode(null, LocalDate.of(2019, 5, 1), LocalDate.of(2020,  1, 31), null), // Overlapp inn i periode
            // Disse tre dekker tilsammen, med litt overlapp
            new StillingPeriode(null, LocalDate.of(2019, 5, 1), LocalDate.of(2020,  1, 31), null), // Dekker
            new StillingPeriode(null, LocalDate.of(2020, 2, 1), LocalDate.of(2020,  5, 15), null), // Dekker
            new StillingPeriode(null, LocalDate.of(2020, 6, 1), LocalDate.of(2021,  2, 28), null), // Her er det hull

            new StillingPeriode(null, LocalDate.of(2020, 9, 1), LocalDate.of(2021, 12, 31), null), // Overlapper på slutten
            new StillingPeriode(null, LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31), null) // Kommer etter
        );

        var aktuellStillingPeriode = new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), null);
        var result = CUT.dekkerEksisterendeStillingPerioderAngittStillingPeriode(eksisterendeStillingPerioder, aktuellStillingPeriode);

        assertFalse(result);
    }

    @Test
    @DisplayName("Test for sammenhengende Stillingperioder i annen StillingPeriodes periode - single dekker ikke starten, flere andre uten overlapp inn i perioden")
    void dekkerEksisterendeStillingPerioderAngittStillingPeriode_SingelDekkerIkkeStarten_FlereAndreUtenOverlapp() {
        List<StillingPeriode> eksisterendeStillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), null), // Før
            // Disse tre dekker tilsammen, med litt overlapp
            new StillingPeriode(null, LocalDate.of(2020, 5, 1), LocalDate.of(2021,  1, 31), null), // Dekker ikke starten

            new StillingPeriode(null, LocalDate.of(2020, 9, 1), LocalDate.of(2021, 12, 31), null), // Overlapper på slutten
            new StillingPeriode(null, LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31), null) // Kommer etter
        );

        var aktuellStillingPeriode = new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), null);
        var result = CUT.dekkerEksisterendeStillingPerioderAngittStillingPeriode(eksisterendeStillingPerioder, aktuellStillingPeriode);

        assertFalse(result);
    }

    @Test
    @DisplayName("Test for sammenhengende Stillingperioder i annen StillingPeriodes periode - single dekker ikke slutten, flere andre uten overlapp inn i perioden")
    void dekkerEksisterendeStillingPerioderAngittStillingPeriode_SingelDekkerIkkeSlutten_FlereAndreUtenOverlapp() {
        List<StillingPeriode> eksisterendeStillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), null), // Før
            // Disse tre dekker tilsammen, med litt overlapp
            new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31), null), // Dekker ikke slutten

            new StillingPeriode(null, LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31), null) // Kommer etter
        );

        var aktuellStillingPeriode = new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), null);
        var result = CUT.dekkerEksisterendeStillingPerioderAngittStillingPeriode(eksisterendeStillingPerioder, aktuellStillingPeriode);

        assertFalse(result);
    }

    @Test
    @DisplayName("Test for sammenhengende Stillingperioder i annen StillingPeriodes periode - single dekker ikke starten eller slutten, flere andre uten overlapp inn i perioden")
    void dekkerEksisterendeStillingPerioderAngittStillingPeriode_SingelDekkerIkkeStartenEllerSlutten_FlereAndreUtenOverlapp() {
        List<StillingPeriode> eksisterendeStillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), null), // Før
            // Disse tre dekker tilsammen, med litt overlapp
            new StillingPeriode(null, LocalDate.of(2020, 5, 1), LocalDate.of(2021, 10, 31), null), // Dekker ikke starten eller slutten

            new StillingPeriode(null, LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31), null) // Kommer etter
        );

        var aktuellStillingPeriode = new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), null);
        var result = CUT.dekkerEksisterendeStillingPerioderAngittStillingPeriode(eksisterendeStillingPerioder, aktuellStillingPeriode);

        assertFalse(result);
    }

    StrukturerteStillingPerioderForNavident DUMMY_sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", new ArrayList<>(), new ArrayList<>());

    @Test
    @DisplayName("Test slå sammen sammenhengende Stillingperioder for samme enhet og DOL - hull i periode - ingen å slå sammen")
    void slaaSammenStillingPerioder_hullIPeriode() {

        IOrgenhet orgenhet = orgenhet("a123bc", "Enhet 1", "1100001");
        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), orgenhet, JA), //
            new StillingPeriode(null, LocalDate.of(2020, 5, 1), LocalDate.of(2021, 10, 31), orgenhet, JA) //
        );

        var result = CUT.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2019, 1, 1), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2019, 12, 31), result.get(0).gyldigTom());
        assertEquals(orgenhet.getId(), result.get(0).orgenhet().getId());
        assertEquals(JA, result.get(0).dagligOppfolging());

        assertEquals(LocalDate.of(2020, 5, 1), result.get(1).gyldigFom());
        assertEquals(LocalDate.of(2021, 10, 31), result.get(1).gyldigTom());
        assertEquals(orgenhet.getId(), result.get(1).orgenhet().getId());
        assertEquals(JA, result.get(1).dagligOppfolging());
    }

    @Test
    @DisplayName("Test slå sammen sammenhengende Stillingperioder for samme enhet og DOL - hull i periode en dag - ingen å slå sammen")
    void slaaSammenStillingPerioder_hullIPeriode_enDag() {
        IOrgenhet orgenhet = orgenhet("a123bc", "Enhet 1", "1100001");
        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 30), orgenhet, JA), //
            new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2021, 10, 31), orgenhet, JA) //
        );

        var result = CUT.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2019, 1, 1), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2019, 12, 30), result.get(0).gyldigTom());
        assertEquals(orgenhet.getId(), result.get(0).orgenhet().getId());
        assertEquals(JA, result.get(0).dagligOppfolging());

        assertEquals(LocalDate.of(2020, 1, 1), result.get(1).gyldigFom());
        assertEquals(LocalDate.of(2021, 10, 31), result.get(1).gyldigTom());
        assertEquals(orgenhet.getId(), result.get(1).orgenhet().getId());
        assertEquals(JA, result.get(1).dagligOppfolging());
    }

    @Test
    @DisplayName("Test slå sammen sammenhengende Stillingperioder for samme enhet og DOL - Sammenhengende i periode - slå sammen")
    void slaaSammenStillingPerioder_sammenhengendePeriode() {
        IOrgenhet orgenhet = orgenhet("a123bc", "Enhet 1", "1100001");
        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), orgenhet, JA), //
            new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2021, 10, 31), orgenhet, JA) //
        );

        var result = CUT.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2019, 1, 1), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2021, 10, 31), result.get(0).gyldigTom());
        assertEquals(orgenhet.getId(), result.get(0).orgenhet().getId());
        assertEquals(JA, result.get(0).dagligOppfolging());
    }

    @Test
    @DisplayName("Test slå sammen sammenhengende Stillingperioder for samme enhet og DOL - Overlappende i periode - slå sammen")
    void slaaSammenStillingPerioder_overlappendePeriode() {
        IOrgenhet orgenhet = orgenhet("a123bc", "Enhet 1", "1100001");
        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), orgenhet, JA), //
            new StillingPeriode(null, LocalDate.of(2019, 5, 1), LocalDate.of(2021, 10, 31), orgenhet, JA) //
        );

        var result = CUT.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2019, 1, 1), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2021, 10, 31), result.get(0).gyldigTom());
        assertEquals(orgenhet.getId(), result.get(0).orgenhet().getId());
        assertEquals(JA, result.get(0).dagligOppfolging());
    }

    @Test
    @DisplayName("Test slå sammen sammenhengende Stillingperioder for samme enhet og DOL - Overlappende i periode en dag - slå sammen")
    void slaaSammenStillingPerioder_overlappendePeriodeEnDag() {
        IOrgenhet orgenhet = orgenhet("a123bc", "Enhet 1", "1100001");
        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2020,  1,  1), orgenhet, JA), //
            new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2021, 10, 31), orgenhet, JA) //
        );

        var result = CUT.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2019, 1, 1), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2021, 10, 31), result.get(0).gyldigTom());
        assertEquals(orgenhet.getId(), result.get(0).orgenhet().getId());
        assertEquals(JA, result.get(0).dagligOppfolging());
    }

    @Test
    @DisplayName("Test slå sammen sammenhengende Stillingperioder for samme enhet og DOL - Hull i periode en dag - ikke slå sammen")
    void slaaSammenStillingPerioder_hullIPeriodeEnDag() {
        IOrgenhet orgenhet = orgenhet("a123bc", "Enhet 1", "1100001");
        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), orgenhet, JA), //
            new StillingPeriode(null, LocalDate.of(2020, 1, 2), LocalDate.of(2021, 10, 31), orgenhet, JA) //
        );

        var result = CUT.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2019, 1, 1), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2019, 12, 31), result.get(0).gyldigTom());
        assertEquals(orgenhet.getId(), result.get(0).orgenhet().getId());
        assertEquals(JA, result.get(0).dagligOppfolging());

        assertEquals(LocalDate.of(2020, 1, 2), result.get(1).gyldigFom());
        assertEquals(LocalDate.of(2021, 10, 31), result.get(1).gyldigTom());
        assertEquals(orgenhet.getId(), result.get(1).orgenhet().getId());
        assertEquals(JA, result.get(1).dagligOppfolging());
    }

    @Test
    @DisplayName("Test slå sammen sammenhengende Stillingperioder for samme enhet og DOL - Sammenhengende i periode - ulike DOL - ingen å slå sammen")
    void slaaSammenStillingPerioder_sammenhengendePeriode_ulikDOL() {
        IOrgenhet orgenhet = orgenhet("a123bc", "Enhet 1", "1100001");
        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), orgenhet, JA), //
            new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2021, 10, 31), orgenhet, NEI) //
        );

        var result = CUT.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2019, 1, 1), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2019, 12, 31), result.get(0).gyldigTom());
        assertEquals(orgenhet.getId(), result.get(0).orgenhet().getId());
        assertEquals(JA, result.get(0).dagligOppfolging());

        assertEquals(LocalDate.of(2020, 1, 1), result.get(1).gyldigFom());
        assertEquals(LocalDate.of(2021, 10, 31), result.get(1).gyldigTom());
        assertEquals(orgenhet.getId(), result.get(1).orgenhet().getId());
        assertEquals(NEI, result.get(1).dagligOppfolging());
    }

    @Test
    @DisplayName("Test slå sammen sammenhengende Stillingperioder for samme enhet og DOL - Sammenhengende i periode - ulike enheter - ingen å slå sammen")
    void slaaSammenStillingPerioder_sammenhengendePeriode_ulikEnhet() {
        IOrgenhet orgenhet = orgenhet("a123bc", "Enhet 1", "1100001");
        IOrgenhet orgenhet2 = orgenhet("c321ba", "Enhet 2", "2200002");
        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), orgenhet, JA), //
            new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2021, 10, 31), orgenhet2, JA) //
        );

        var result = CUT.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2019, 1, 1), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2019, 12, 31), result.get(0).gyldigTom());
        assertEquals(orgenhet.getId(), result.get(0).orgenhet().getId());
        assertEquals(JA, result.get(0).dagligOppfolging());

        assertEquals(LocalDate.of(2020, 1, 1), result.get(1).gyldigFom());
        assertEquals(LocalDate.of(2021, 10, 31), result.get(1).gyldigTom());
        assertEquals(orgenhet2.getId(), result.get(1).orgenhet().getId());
        assertEquals(JA, result.get(1).dagligOppfolging());
    }

    @Test
    @DisplayName("Test slå sammen sammenhengende Stillingperioder for samme enhet og DOL - Mye blandet drops og usortert")
    void slaaSammenStillingPerioder_blandetDrops() {
        IOrgenhet orgenhet1 = orgenhet("a123bc", "Enhet 1", "1100001");
        IOrgenhet orgenhet2 = orgenhet("c321ba", "Enhet 2", "2200002");
        IOrgenhet orgenhet3 = orgenhet("x098yz", "Enhet 3", "3300003");

        List<StillingPeriode> stillingPerioder = new ArrayList<>(List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), orgenhet1, JA), // Enhet 1: 1. Henger sammen
            new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2021,  7, 31), orgenhet1, JA), // Enhet 1: 1. Henger sammen
            new StillingPeriode(null, LocalDate.of(2021, 5, 1), LocalDate.of(2022, 10, 31), orgenhet1, JA), // Enhet 1: 1. Henger sammen - overlapp

            new StillingPeriode(null, LocalDate.of(2022,11, 1), LocalDate.of(2022, 12, 31), orgenhet1, NEI), // Enhet 1: 2. Ulik DOL

            new StillingPeriode(null, LocalDate.of(2023, 1, 2), LocalDate.of(2023, 12, 31), orgenhet1, NEI), // Enhet 1: 3. 1 dag hull

            new StillingPeriode(null, LocalDate.of(2020, 1, 1), LocalDate.of(2021, 10, 31), orgenhet2, JA), // Enhet 2: 1. Single SP

            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2021, 10, 31), orgenhet3, UKJENT), // Enhet 3: 1. Ukjent DOL - henger sammen
            new StillingPeriode(null, LocalDate.of(2021,11, 1), LocalDate.of(2021, 12, 31), orgenhet3, UKJENT), // Enhet 3: 1. Ukjent DOL - henger sammen

            new StillingPeriode(null, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), orgenhet3, UKJENT) // Enhet 3: 1. Ukjent DOL - henger ikke sammen
        ));

        Collections.shuffle(stillingPerioder); // Randomiser rekkefølgen

        var result = CUT.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

        assertEquals(6, result.size());

        assertEquals(LocalDate.of(2019,  1,  1), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2022, 10, 31), result.get(0).gyldigTom());
        assertEquals(orgenhet1.getId(),          result.get(0).orgenhet().getId());
        assertEquals(JA,                         result.get(0).dagligOppfolging());

        assertEquals(LocalDate.of(2019,  1,  1), result.get(1).gyldigFom());
        assertEquals(LocalDate.of(2021, 12, 31), result.get(1).gyldigTom());
        assertEquals(orgenhet3.getId(),          result.get(1).orgenhet().getId());
        assertEquals(UKJENT,                     result.get(1).dagligOppfolging());

        assertEquals(LocalDate.of(2020,  1,  1), result.get(2).gyldigFom());
        assertEquals(LocalDate.of(2021, 10, 31), result.get(2).gyldigTom());
        assertEquals(orgenhet2.getId(),          result.get(2).orgenhet().getId());
        assertEquals(JA,                         result.get(2).dagligOppfolging());

        assertEquals(LocalDate.of(2022, 11,  1), result.get(3).gyldigFom());
        assertEquals(LocalDate.of(2022, 12, 31), result.get(3).gyldigTom());
        assertEquals(orgenhet1.getId(),          result.get(3).orgenhet().getId());
        assertEquals(NEI,                        result.get(3).dagligOppfolging());

        assertEquals(LocalDate.of(2023,  1,  1), result.get(4).gyldigFom());
        assertEquals(LocalDate.of(2023, 12, 31), result.get(4).gyldigTom());
        assertEquals(orgenhet3.getId(),          result.get(4).orgenhet().getId());
        assertEquals(UKJENT,                     result.get(4).dagligOppfolging());

        assertEquals(LocalDate.of(2023,  1,  2), result.get(5).gyldigFom());
        assertEquals(LocalDate.of(2023, 12, 31), result.get(5).gyldigTom());
        assertEquals(orgenhet1.getId(),          result.get(5).orgenhet().getId());
        assertEquals(NEI,                        result.get(5).dagligOppfolging());
    }
}