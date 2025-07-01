package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.NO_END_DATE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.today;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.AGRESSOID_PAA_STILLING_ER_IKKE_PRESIS_MEN_BRUKER_EKSISTERENDE_OT_SOM_MATCHER;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_DELPERIODE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_MATCHER__MOT_EKSISTERENDE_OT;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_MEN_PERIDOE_ER_DEKT;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_OG_HAR_INGEN_ANDRE_STILLINGSPERIODER_ELLER_ORGTILKNYTNINGER;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.FLERE_ENHETER_KAN_MATCHES_FOR_DEL_AV_STILLINGSPERIODE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.KAN_IKKE_MATCHE_STILLINGSPERIODE_TIL_ENHET;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_PERIODE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.statlige.Util.sorterOrgenhetFomDatoerNyesteDatoFoerstPrefererLiktNavn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

// REF: no/nav/nom/apps/api/orgtilknytning/dvh/dokumentasjon/TestReferanser_MatcheTilEnheterINOM.md
@Slf4j
@DisplayName("Tester matching av RessursStillinger fra DVH til NOM enhet")
class MatchStrukturerteStillingPerioderTilNOMTest extends TestBase {

    @Autowired
    MatchStrukturerteStillingPerioderTilNOM CUT; // Class under test

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

    @Test
    @DisplayName("sorterOrgenhetFomDatoerNyesteDatoFoerstPrefererLiktNavn")
    void test_sorterOrgenhetFomDatoerNyesteDatoFoerstPrefererLiktNavn() {
        var orgenhet1 = orgenhet("a123bc", "Nytt Navn", "0110001", LocalDate.of(2022, 1, 1), null);
        var orgenhet2 = orgenhet("c321a", "Gammelt navn", "0110001", LocalDate.of(2021, 1, 1), null);
        var result = sorterOrgenhetFomDatoerNyesteDatoFoerstPrefererLiktNavn(List.of(orgenhet1, orgenhet2), "Nytt Navn");

        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2022, 1, 1), result.getFirst());
        assertEquals(LocalDate.of(2021, 1, 1), result.getLast());
    }

    @Test
    @DisplayName("Singel match på presis AgressoId på Stillingen - Ref: 1.i.a.a")
    void test_SingleMatchPresisAgressoId_1_i_a_a() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Single match", LocalDate.of(2022, 1, 1), MAX_DATE)
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(orgenhet("a123bc", "Single match", "0110001", LocalDate.of(2022, 1, 1), null)));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("0110001",                 forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("Single match",            forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET",                forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                  forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
    }

    @Test
    @DisplayName("Singel match på upresis AgressoId matcher presis AgressoId på Ressurs på Stillingen - Ref: 2.i -> 1.i.a.a")
    void test_SingleMatchUpresisAgressoIdMatcherPresisRessursAgressoId_2_i__1_i_a_a() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("01100", "0110001", "Single match", LocalDate.of(2022, 1, 1), MAX_DATE)
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(orgenhet("a123bc", "Single match", "0110001", LocalDate.of(2022, 1, 1), null)));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals("01100",                   forsteStillingPeriode.stilling().agressoId().orElse(null));
        assertEquals("0110001",                 forsteStillingPeriode.stilling().ressursAgressoId().orElse(null));
        assertEquals("Single match",            forsteStillingPeriode.stilling().enhetNavn());
        assertEquals("ORGENHET",                forsteStillingPeriode.stilling().ressursAgressoOrgNiv().orElse(null));
        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                  forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
    }

    @Test
    @DisplayName("Singel match på presis AgressoId på Stillingen - Ref: 1.i.a.b")
    void test_SingleMatchPresisAgressoId_1_i_a_b() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Single match", LocalDate.of(2023, 1, 1), MAX_DATE)
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(orgenhet("a123bc", "Single match", "0110001")));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2023, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                  forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
    }

    @Test
    @DisplayName("Singel match på presis AgressoId med navn på Stillingen - Ref: 1.i.a.b")
    void test_SingleMatchPresisAgressoId_medNavn_1_i_a_b() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Single match", LocalDate.of(2023, 1, 1), MAX_DATE)
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a999bc", "Single match", "0110001", LocalDate.of(2021, 1, 1), null), // Skal velge den under fordi den er nyere
            orgenhet("a123bc", "Single match", "0110001", LocalDate.of(2023, 1, 1), null),
            orgenhet("c987ba", "Annet navn", "0110001", LocalDate.of(2024, 1, 1), null), // Selv om denne er nyere - skal den velge match på navn først.
            orgenhet("c321ba", "Annet navn", "0110001")
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2023, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                  forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
    }


    @Test
    @DisplayName("Singel match på upresis AgressoId matcher presis AgressoId på Ressurs på Stillingen - Ref: 2.i -> 1.i.a.b")
    void test_SingleMatchUpresisAgressoIdMatcherPresisRessursAgressoId_2_i__1_i_a_b() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("01100", "0110001", "Single match", LocalDate.of(2023, 1, 1), MAX_DATE)
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(orgenhet("a123bc", "Single match", "0110001")));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2023, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(MAX_DATE,                  forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
    }

    @Test
    @DisplayName("Singel match på presis AgressoId på Stillingen - Ref: 1.i.a.b")
    void test_SingleMatchPresisAgressoId_1_i_a_c() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Single match", LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(orgenhet("a123bc", "Single match", "0110001")));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2023, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
    }


    @Test
    @DisplayName("Singel match på presis AgressoId på Stillingen - perioden til enheten dekker ikke hele perioden - Ref: 1.i.b.a")
    void test_SingleMatchPresisAgressoId_EnhetPeriodeDekkerIkke_1_i_b_a() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Single match", LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Single match", "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 12, 31)))
        );

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(1, result.errors().size());
        assertEquals(STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_PERIODE, result.errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Singel match på presis AgressoId på Stillingen - perioden til enheten dekker ikke hele perioden - Ref: 1.i.b.b")
    void test_SingleMatchPresisAgressoId_EnhetPeriodeDekkerIkke_1_i_b_b() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Single match", LocalDate.of(2021, 1, 1), LocalDate.of(2026, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Single match", "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 12, 31)))
        );

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 12, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(1, result.errors().size());
        assertEquals(STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_PERIODE, result.errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Singel match på presis AgressoId på Stillingen - perioden til enheten dekker ikke hele perioden - Ref: 1.i.b.c")
    void test_SingleMatchPresisAgressoId_EnhetPeriodeDekkerIkke_1_i_b_c() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Single match", LocalDate.of(2023, 1, 1), LocalDate.of(2026, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Single match", "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 12, 31)))
        );

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2023, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 12, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(1, result.errors().size());
        assertEquals(STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_PERIODE, result.errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Singel match på upresis AgressoId matcher presis AgressoId på Ressurs på Stillingen - perioden til enheten dekker ikke hele perioden - Ref: 2.i -> 1.i.b.c")
    void test_SingleMatchUpresisAgressoIdMatcherPresisRessursAgressoId_EnhetPeriodeDekkerIkke_2_i__1_i_b_c() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("01100", "0110001", "Single match", LocalDate.of(2023, 1, 1), LocalDate.of(2026, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Single match", "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 12, 31)))
        );

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2023, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 12, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(1, result.errors().size());
        assertEquals(STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_PERIODE, result.errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Singel match på presis AgressoId på Stillingen - perioden til enheten dekker ikke hele perioden - match mot eksisterende OTer - Ref: 1.i.c")
    void test_SingleMatchPresisAgressoId_EnhetPeriodeDekkerIkke_MatchMotEksisterendeOTer_1_i_c() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Single match", LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Single match", "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 12, 31)))
        );

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31))
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2021, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(1, result.errors().size());
        assertEquals(STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT, result.errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Singel match på presis AgressoId på Stillingen - perioden til enheten dekker ikke hele perioden " +
        "- match mot eksisterende OTer - som ikke dekker hele perioden " +
        "- kombinasjonen dekker hele perioden - Ref: 1.i.c.a")
    void test_SingleMatchPresisAgressoId_EnhetPeriodeDekkerIkke_MatchMotEksisterendeOTer_KombinasjonenDekkerHelePerioden_1_i_c_a() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Single match", LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Single match", "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 12, 31)))
        );

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2021, 1, 1), LocalDate.of(2023, 1, 31))
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2021, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(1, result.errors().size());
        assertEquals(STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT, result.errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Singel match på presis AgressoId på Stillingen - perioden til enheten dekker ikke hele perioden " +
        "- match mot eksisterende OTer - som ikke dekker hele perioden " +
        "- kombinasjonen dekker ikke hele perioden - Ref: 1.i.c.b")
    void test_SingleMatchPresisAgressoId_EnhetPeriodeDekkerIkke_MatchMotEksisterendeOTer_KombinasjonenDekkerIkkeHelePerioden_1_i_c_b() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Single match", LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Single match", "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 12, 31)))
        );

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2021, 5, 1), LocalDate.of(2025, 1, 31))
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2021, 5, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(1, result.errors().size());
        assertEquals(STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT, result.errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Singel match på presis AgressoId på Stillingen - perioden til enheten dekker ikke hele perioden " +
        "- match mot eksisterende OTer - som ikke dekker hele perioden " +
        "- kombinasjonen dekker ikke hele perioden - Ref: 1.i.c.c")
    void test_SingleMatchPresisAgressoId_EnhetPeriodeDekkerIkke_MatchMotEksisterendeOTer_KombinasjonenDekkerIkkeHelePerioden_1_i_c_c() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Single match", LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Single match", "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 12, 31)))
        );

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2021, 5, 1), LocalDate.of(2023, 1, 31))
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2021, 5, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(1, result.errors().size());
        assertEquals(STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT, result.errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Singel match på presis AgressoId på Stillingen - perioden til enheten dekker ikke hele perioden " +
        "- match mot flere eksisterende OTer - kombinasjonen dekker hele perioden - Ref: 1.i.c.d")
    void test_SingleMatchPresisAgressoId_EnhetPeriodeDekkerIkke_MatchMotFlereEksisterendeOTer_KombinasjonenDekkerHelePerioden_1_i_c_d() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Single match", LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Single match", "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 12, 31)))
        );

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 31)),
            orgtilknytning(A000001, "a123bc", LocalDate.of(2022, 2, 1), LocalDate.of(2024, 1, 31))
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2021, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(1, result.errors().size());
        assertEquals(STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT, result.errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Singel match på presis AgressoId på Stillingen - perioden til enheten dekker ikke hele perioden " +
        "- match mot flere eksisterende OTer - Foran og bak - kombinasjonen dekker hele perioden - Ref: 1.i.c.e")
    void test_SingleMatchPresisAgressoId_EnhetPeriodeDekkerIkke_MatchMotFlereEksisterendeOTer_ForenOgBak_KombinasjonenDekkerHelePerioden_1_i_c_e() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Single match", LocalDate.of(2021, 1, 1), LocalDate.of(2025, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Single match", "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 12, 31)))
        );

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 31)),
            orgtilknytning(A000001, "a123bc", LocalDate.of(2022, 2, 1), LocalDate.of(2026, 1, 31))
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2021, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2025, 1, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(1, result.errors().size());
        assertEquals(STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT, result.errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Singel match på presis AgressoId på Stillingen - perioden til enheten dekker ikke hele perioden " +
        "- match mot flere eksisterende OTer - Foran og bak - kombinasjonen dekker ikke hele perioden - Ref: 1.i.c.f")
    void test_SingleMatchPresisAgressoId_EnhetPeriodeDekkerIkke_MatchMotFlereEksisterendeOTer_ForenOgBak_KombinasjonenDekkerIkkeHelePerioden_1_i_c_f() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Single match", LocalDate.of(2021, 1, 1), LocalDate.of(2025, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Single match", "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 12, 31)))
        );

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2021, 5, 1), LocalDate.of(2022, 1, 31)),
            orgtilknytning(A000001, "a123bc", LocalDate.of(2022, 2, 1), LocalDate.of(2026, 1, 31))
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2021, 5, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2025, 1, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(1, result.errors().size());
        assertEquals(STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT, result.errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Multi match mot enhet på presis AgressoId på Stillingen - perioden til enhetene dekker hele perioden - Ref: 1.ii.a.a")
    void test_MultiMatchPresisAgressoId_EnhetPerioderDekker_1_ii_a_a() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Nytt navn", LocalDate.of(2022, 1, 1), null)
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2023, 1, 1), null)
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(Collections.emptyList());

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();
        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 12, 31),forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(LocalDate.of(2023, 1, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(null,                      andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());
        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Multi match mot enhet på presis AgressoId på Stillingen - perioden til enhetene dekker hele perioden - Ref: 1.ii.a.b")
    void test_MultiMatchPresisAgressoId_EnhetPerioderDekker_1_ii_a_b() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Nytt navn", LocalDate.of(2022, 5, 1), null)
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2023, 1, 1), null)
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(Collections.emptyList());

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();
        assertEquals(LocalDate.of(2022, 5, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 12, 31),forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(LocalDate.of(2023, 1, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(null,                      andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());
        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Multi match mot enhet på presis AgressoId på Stillingen - perioden til enhetene dekker hele perioden - Ref: 1.ii.a.c")
    void test_MultiMatchPresisAgressoId_EnhetPerioderDekker_1_ii_a_c() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Nytt navn", LocalDate.of(2022, 5, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2023, 1, 1), null)
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(Collections.emptyList());

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();
        assertEquals(LocalDate.of(2022, 5, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 12, 31),forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(LocalDate.of(2023, 1, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());
        assertEquals(0, result.errors().size());
    }

    @Test
    @DisplayName("Multi match mot enhet på presis AgressoId på Stillingen - perioden til enhetene dekker hele perioden, men overlapper hverandre - Ref: 1.ii.b.a")
    void test_MultiMatchPresisAgressoId_EnhetPerioderDekkerMenOverlapper_1_ii_b_a() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Nytt navn", LocalDate.of(2022, 1, 1), null)
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), null)
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(Collections.emptyList());

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();
        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 6, 30), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(LocalDate.of(2022, 7, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(null,                      andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());
        assertEquals(1, result.errors().size());
        assertEquals(FLERE_ENHETER_KAN_MATCHES_FOR_DEL_AV_STILLINGSPERIODE, result.errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Multi match mot enhet på presis AgressoId på Stillingen - perioden til enhetene dekker hele perioden, men overlapper hverandre - Ref: 1.ii.b.b")
    void test_MultiMatchPresisAgressoId_EnhetPerioderDekkerMenOverlapper_1_ii_b_b() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Nytt navn", LocalDate.of(2022, 5, 1), null)
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), null)
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(Collections.emptyList());

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();
        assertEquals(LocalDate.of(2022, 5, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 6, 30), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(LocalDate.of(2022, 7, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(null,                      andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());
        assertEquals(1, result.errors().size());
        assertEquals(FLERE_ENHETER_KAN_MATCHES_FOR_DEL_AV_STILLINGSPERIODE, result.errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Multi match mot enhet på presis AgressoId på Stillingen - perioden til enhetene dekker hele perioden, men overlapper hverandre - Ref: 1.ii.b.c")
    void test_MultiMatchPresisAgressoId_EnhetPerioderDekkerMenOverlapper_1_ii_b_c() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Nytt navn", LocalDate.of(2022, 5, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), null)
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(Collections.emptyList());

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();
        assertEquals(LocalDate.of(2022, 5, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 6, 30), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(LocalDate.of(2022, 7, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());
        assertEquals(1, result.errors().size());
        assertEquals(FLERE_ENHETER_KAN_MATCHES_FOR_DEL_AV_STILLINGSPERIODE, strukturerteStillingPerioder.errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Multi match mot enhet på presis AgressoId på Stillingen - perioden til enhetene dekker hele perioden, men overlapper hverandre - Ref: 1.ii.b.e")
    void test_MultiMatchPresisAgressoId_EnhetPerioderDekkerMenOverlapper_1_ii_b_e() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Nytt navn", LocalDate.of(2022, 8, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), null)
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(Collections.emptyList());

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2022, 8, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  forsteStillingPeriode.orgenhet().getId());
        assertEquals(1, result.errors().size());
        assertEquals(FLERE_ENHETER_KAN_MATCHES_FOR_DEL_AV_STILLINGSPERIODE, result.errors().getFirst().errorType());
    }

    @Test
    @DisplayName("Multi match mot enhet på presis AgressoId på Stillingen - perioden til enhetene dekker ikke hele perioden - Ref: 1.ii.c.a")
    void test_MultiMatchPresisAgressoId_EnhetPerioderDekkerIkke_1_ii_c_a() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Nytt navn", LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 12, 31))
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(Collections.emptyList());

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();

        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 6, 30), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(LocalDate.of(2022, 7, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());

        assertEquals(2, result.errors().size());
        assertEquals(FLERE_ENHETER_KAN_MATCHES_FOR_DEL_AV_STILLINGSPERIODE,                 result.errors().getFirst().errorType());
        assertEquals(DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_DELPERIODE, result.errors().getLast().errorType());
    }

    @Test
    @DisplayName("Multi match mot enhet på presis AgressoId på Stillingen - perioden til enhetene dekker ikke hele perioden - Ref: 1.ii.c.b")
    void test_MultiMatchPresisAgressoId_EnhetPerioderDekkerIkke_1_ii_c_b() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Nytt navn", LocalDate.of(2021, 1, 1), LocalDate.of(2026, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 12, 31))
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(Collections.emptyList());

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        var andreStillingPeriode = result.stillingPerioder().getLast();

        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 6, 30), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(LocalDate.of(2022, 7, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 12, 31),andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());

        assertEquals(3, result.errors().size());
        assertEquals(1, result.countOfErrorType(FLERE_ENHETER_KAN_MATCHES_FOR_DEL_AV_STILLINGSPERIODE));
        assertEquals(2, result.countOfErrorType(DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_DELPERIODE));
    }

    @Test
    @DisplayName("Multi match mot enhet på presis AgressoId på Stillingen - perioden til enhetene dekker ikke hele perioden - Ref: 1.ii.c.c")
    void test_MultiMatchPresisAgressoId_EnhetPerioderDekkerIkke_1_ii_c_c() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Nytt navn", LocalDate.of(2023, 1, 1), LocalDate.of(2026, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 12, 31))
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(Collections.emptyList());

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();

        assertEquals(LocalDate.of(2023, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 12, 31),forsteStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_PERIODE));
    }

    @Test
    @DisplayName("Multi match mot enhet på upresis AgressoId matcher presis AgressoId på Ressurs på Stillingen " +
        "- perioden til enhetene dekker ikke hele perioden - Ref: 2.i -> 1.ii.c.c")
    void test_MultiMatchUpresisAgressoIdMatcherPresisRessursAgressoId_EnhetPerioderDekkerIkke_2_i__1_ii_c_c() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("01100", "0110001", "Nytt navn", LocalDate.of(2023, 1, 1), LocalDate.of(2026, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 12, 31))
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(Collections.emptyList());

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();

        assertEquals(LocalDate.of(2023, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 12, 31),forsteStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_PERIODE));
    }

    @Test
    @DisplayName("Multi match mot enhet på presis AgressoId på Stillingen - perioden til enhetene dekker ikke hele perioden " +
        "- match på eksisterende OTer- Ref: 1.ii.d.a")
    void test_MultiMatchPresisAgressoId_EnhetPerioderDekkerIkke_MatchEksisterendeOTer_1_ii_d_a() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Nytt navn", LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 12, 31))
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2021, 1, 1), LocalDate.of(2022, 6, 30)),
            orgtilknytning(A000001, "c321ba", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 1, 31))
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().get(0);
        var andreStillingPeriode = result.stillingPerioder().get(1);

        assertEquals(LocalDate.of(2021, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 6, 30), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(LocalDate.of(2022, 7, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());

        assertEquals(2, result.errors().size());
        assertEquals(1, result.countOfErrorType(FLERE_ENHETER_KAN_MATCHES_FOR_DEL_AV_STILLINGSPERIODE));
        assertEquals(1, result.countOfErrorType(DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT));
    }

    @Test
    @DisplayName("Multi match mot enhet på bupresis AgressoId matcher presis AgressoId på Ressurs på Stillingen " +
        "- perioden til enhetene dekker ikke hele perioden " +
        "- match på eksisterende OTer- Ref: 2.i -> 1.ii.d.a")
    void test_MultiMatchUpresisAgressoIdMatcherPresisRessursAgressoId_EnhetPerioderDekkerIkke_MatchEksisterendeOTer_2_i__1_ii_d_a() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("01100", "0110001", "Nytt navn", LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 12, 31))
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2021, 1, 1), LocalDate.of(2022, 6, 30)),
            orgtilknytning(A000001, "c321ba", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 1, 31))
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().get(0);
        var andreStillingPeriode = result.stillingPerioder().get(1);

        assertEquals(LocalDate.of(2021, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 6, 30), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(LocalDate.of(2022, 7, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());

        assertEquals(2, result.errors().size());
        assertEquals(1, result.countOfErrorType(FLERE_ENHETER_KAN_MATCHES_FOR_DEL_AV_STILLINGSPERIODE));
        assertEquals(1, result.countOfErrorType(DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT));
    }

    @Test
    @DisplayName("Multi match mot enhet på presis AgressoId på Stillingen - perioden til enhetene dekker ikke hele perioden " +
        "- match på eksisterende OTer - bruk kombinasjon av enhet/OT periode - Ref: 1.ii.d.b.a")
    void test_MultiMatchPresisAgressoId_EnhetPerioderDekkerIkke_MatchEksisterendeOTer_1_ii_d_b_a() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Nytt navn", LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 12, 31))
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2021, 1, 1), LocalDate.of(2022, 6, 30)),
            orgtilknytning(A000001, "c321ba", LocalDate.of(2022, 7, 1), LocalDate.of(2023, 1, 31))
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().get(0);
        var andreStillingPeriode = result.stillingPerioder().get(1);

        assertEquals(LocalDate.of(2021, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 6, 30), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(LocalDate.of(2022, 7, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());

        assertEquals(2, result.errors().size());
        assertEquals(1, result.countOfErrorType(FLERE_ENHETER_KAN_MATCHES_FOR_DEL_AV_STILLINGSPERIODE));
        assertEquals(1, result.countOfErrorType(DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT));
    }

    @Test
    @DisplayName("Multi match mot enhet på presis AgressoId på Stillingen - perioden til enhetene dekker ikke hele perioden " +
        "- match på eksisterende OTer - bruk kombinasjon av enhet/OT periode - Ref: 1.ii.d.b.b")
    void test_MultiMatchPresisAgressoId_EnhetPerioderDekkerIkke_MatchEksisterendeOTer_1_ii_d_b_b() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Nytt navn", LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 12, 31))
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2021, 5, 1), LocalDate.of(2022, 6, 30)),
            orgtilknytning(A000001, "c321ba", LocalDate.of(2022, 7, 1), LocalDate.of(2023, 1, 31))
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().get(0);
        var andreStillingPeriode = result.stillingPerioder().get(1);

        assertEquals(LocalDate.of(2021, 5, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 6, 30), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(LocalDate.of(2022, 7, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());

        assertEquals(3, result.errors().size());
        assertEquals(1, result.countOfErrorType(FLERE_ENHETER_KAN_MATCHES_FOR_DEL_AV_STILLINGSPERIODE));
        assertEquals(1, result.countOfErrorType(DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT));
        assertEquals(1, result.countOfErrorType(DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_DELPERIODE));
    }

    @Test
    @DisplayName("Multi match mot enhet på presis AgressoId på Stillingen - perioden til enhetene dekker ikke hele perioden " +
        "- match på eksisterende OTer - bruk kombinasjon av enhet/OT periode - Ref: 1.ii.d.b.c")
    void test_MultiMatchPresisAgressoId_EnhetPerioderDekkerIkke_MatchEksisterendeOTer_1_ii_d_b_c() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Nytt navn", LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 12, 31))
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2021, 5, 1), LocalDate.of(2022, 9, 30)),
            orgtilknytning(A000001, "c321ba", LocalDate.of(2022, 7, 1), LocalDate.of(2023, 1, 31))
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().get(0);
        var andreStillingPeriode = result.stillingPerioder().get(1);

        assertEquals(LocalDate.of(2021, 5, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 6, 30), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(LocalDate.of(2022, 7, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());

        assertEquals(3, result.errors().size());
        assertEquals(1, result.countOfErrorType(FLERE_ENHETER_KAN_MATCHES_FOR_DEL_AV_STILLINGSPERIODE));
        assertEquals(1, result.countOfErrorType(DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT));
        assertEquals(1, result.countOfErrorType(DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_DELPERIODE));
    }

    @Test
    @DisplayName("Multi match mot enhet på presis AgressoId på Stillingen - perioden til enhetene dekker ikke hele perioden " +
        "- ingen match på eksisterende OTer - bruk kombinasjon av enhet/OT periode - Ref: 1.ii.d.b.d")
    void test_MultiMatchPresisAgressoId_EnhetPerioderDekkerIkke_MatchEksisterendeOTer_1_ii_d_b_d() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0110001", "Nytt navn", LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 12, 31))
        ));


        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "x123xx", LocalDate.of(2021, 5, 1), LocalDate.of(2022, 9, 30)),    // ANNEN ENHET
            orgtilknytning(A000001, "x321xx", LocalDate.of(2022, 7, 1), LocalDate.of(2023, 1, 31))     // ANNEN ENHET
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().get(0);
        var andreStillingPeriode = result.stillingPerioder().get(1);

        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 6, 30), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(LocalDate.of(2022, 7, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());

        assertEquals(2, result.errors().size());
        assertEquals(1, result.countOfErrorType(FLERE_ENHETER_KAN_MATCHES_FOR_DEL_AV_STILLINGSPERIODE));
        assertEquals(1, result.countOfErrorType(DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_DELPERIODE));
    }

    @Test
    @DisplayName("INGEN match mot enhet på presis AgressoId på Stillingen - andre stillingPerioder dekker denne " +
        "- ikke bruk - Ref: 1.iii.a")
    void test_IngenMatchPresisAgressoId_andreStillingPeriodeDekker_iii_a() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0XX000X", "UKJENT"      , LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 31)),
            stillingPeriode("0110001", "Gammelt navn", LocalDate.of(2021, 1, 1), LocalDate.of(2022, 6, 30)),
            stillingPeriode("0110001", "Nytt navn"   , LocalDate.of(2022, 7, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 6, 30)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 1, 31))
        ));

        when(orgenhetService.exactMatchByAgressoId("0XX000X")).thenReturn(Collections.emptyList());

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().get(0);
        var andreStillingPeriode = result.stillingPerioder().get(1);

        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 6, 30), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(LocalDate.of(2022, 7, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());

        assertEquals(2, result.errors().size());
        assertEquals(1, result.countOfErrorType(STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_PERIODE));
        assertEquals(1, result.countOfErrorType(FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_MEN_PERIDOE_ER_DEKT));
    }

    @Test
    @DisplayName("INGEN match mot enhet på upresis AgressoId matcher presis AgressoId på Ressurs på Stillingen - andre stillingPerioder dekker denne " +
        "- ikke bruk - Ref: 2.i -> 1.iii.a")
    void test_IngenMatchUpresisAgressoIdMatcherPresisRessursAgressoId_andreStillingPeriodeDekker_2_i__iii_a() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0XX00", "0XX000X", "UKJENT"      , LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 31)),
            stillingPeriode("01100", "0110001", "Gammelt navn", LocalDate.of(2021, 1, 1), LocalDate.of(2022, 6, 30)),
            stillingPeriode("01100", "0110001", "Nytt navn"   , LocalDate.of(2022, 7, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 6, 30)),
            orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 1, 31))
        ));

        when(orgenhetService.exactMatchByAgressoId("0XX000X")).thenReturn(Collections.emptyList());

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().get(0);
        var andreStillingPeriode = result.stillingPerioder().get(1);

        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 6, 30), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(LocalDate.of(2022, 7, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());

        assertEquals(2, result.errors().size());
        assertEquals(1, result.countOfErrorType(STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_PERIODE));
        assertEquals(1, result.countOfErrorType(FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_MEN_PERIDOE_ER_DEKT));
    }

    @Test
    @DisplayName("INGEN match mot enhet på presis AgressoId på Stillingen - ingen andre stillingPerioder dekker denne " +
        "- Finnes eksisterende OT for perioden - bruk OT'en - Ref: 1.iii.b")
    void test_IngenMatchPresisAgressoId_ingenAndreStillingPeriodeDekker_OTForPeriodeFinnes_iii_b() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0XX000X", "UKJENT"      , LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        var orgenhet1 = orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 6, 30));
        var orgenhet2 = orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 1, 31));

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(orgenhet1, orgenhet2));

        when(orgenhetService.exactMatchByAgressoId("0XX000X")).thenReturn(Collections.emptyList());
        when(orgenhetService.getById("a123bc")).thenReturn(Optional.of(orgenhet1));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 31))
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().get(0);

        assertEquals(LocalDate.of(2023, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_MATCHER__MOT_EKSISTERENDE_OT));
    }

    @Test
    @DisplayName("INGEN match mot enhet på presis AgressoId på Stillingen - andre stillingPerioder dekker delvis denne " +
        "- Finnes eksisterende OT for perioden - bruk OT'en - Ref: 1.iii.c")
    void test_IngenMatchPresisAgressoId_AndreStillingPeriodeDekkerDelvis_OTForPeriodeFinnes_iii_c() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0XX000X", "UKJENT"      , LocalDate.of(2022, 1, 1), LocalDate.of(2024, 1, 31)),
            stillingPeriode("0110001", "Gammelt navn", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 6, 30))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        var orgenhet1 = orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 6, 30));
        var orgenhet2 = orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 1, 31));

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(orgenhet1, orgenhet2));

        when(orgenhetService.exactMatchByAgressoId("0XX000X")).thenReturn(Collections.emptyList());
        when(orgenhetService.getById("a123bc")).thenReturn(Optional.of(orgenhet1));
        when(orgenhetService.getById("c321ba")).thenReturn(Optional.of(orgenhet2));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "c321ba", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 1, 31))
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(2, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().get(0);
        var andreStillingPeriode  = result.stillingPerioder().get(1);

        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 6, 30), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(LocalDate.of(2022, 1, 1),  andreStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), andreStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  andreStillingPeriode.orgenhet().getId());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_MATCHER__MOT_EKSISTERENDE_OT));
    }

    @Test
    @DisplayName("INGEN match mot enhet på presis AgressoId på Stillingen - ingen andre stillingPerioder dekker denne " +
        "- Finnes flere eksisterende OT for perioden - bruk DOL OT'en - Ref: 1.iii.d")
    void test_IngenMatchPresisAgressoId_ingenAndreStillingPeriodeDekker_OTForPeriodeFinnes_brukDOL_iii_d() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0XX000X", "UKJENT"      , LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        var orgenhet1 = orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 6, 30));
        var orgenhet2 = orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 1, 31));

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(orgenhet1, orgenhet2));

        when(orgenhetService.exactMatchByAgressoId("0XX000X")).thenReturn(Collections.emptyList());
        when(orgenhetService.getById("a123bc")).thenReturn(Optional.of(orgenhet1));
        when(orgenhetService.getById("c321ba")).thenReturn(Optional.of(orgenhet2));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 31), false),
            orgtilknytning(A000001, "c321ba", LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 31), true)
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().get(0);

        assertEquals(LocalDate.of(2023, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("c321ba",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_MATCHER__MOT_EKSISTERENDE_OT));
    }

    @Test
    @DisplayName("INGEN match mot enhet på presis AgressoId på Stillingen - ingen andre stillingPerioder dekker denne " +
        "- Finnes flere eksisterende OT for perioden - ingen DOL - bruk første OT'en - Ref: 1.iii.e")
    void test_IngenMatchPresisAgressoId_ingenAndreStillingPeriodeDekker_OTForPeriodeFinnes_ingenDOL_brukFoerste_iii_e() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0XX000X", "UKJENT"      , LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        var orgenhet1 = orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 6, 30));
        var orgenhet2 = orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 1, 31));

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(orgenhet1, orgenhet2));

        when(orgenhetService.exactMatchByAgressoId("0XX000X")).thenReturn(Collections.emptyList());
        when(orgenhetService.getById("a123bc")).thenReturn(Optional.of(orgenhet1));
        when(orgenhetService.getById("c321ba")).thenReturn(Optional.of(orgenhet2));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 31), false),
            orgtilknytning(A000001, "c321ba", LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 31), false)
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().get(0);

        assertEquals(LocalDate.of(2023, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31), forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_MATCHER__MOT_EKSISTERENDE_OT));
    }

    @Test
    @DisplayName("INGEN match mot enhet på presis AgressoId på Stillingen - ingen andre stillingPerioder dekker denne " +
        "- ingen eksisterende OT for perioden - ikke bruk - Ref: 1.iii.f")
    void test_IngenMatchPresisAgressoId_ingenAndreStillingPeriodeDekker_IngenOTForPeriodeFinnes_ikkeBruk_iii_e() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("0XX000X", "UKJENT"      , LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        var orgenhet1 = orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 6, 30));
        var orgenhet2 = orgenhet("c321ba", "Nytt navn",     "0110001", LocalDate.of(2022, 7, 1), LocalDate.of(2024, 1, 31));

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(orgenhet1, orgenhet2));

        when(orgenhetService.exactMatchByAgressoId("0XX000X")).thenReturn(Collections.emptyList());

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(0, result.stillingPerioder().size());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_OG_HAR_INGEN_ANDRE_STILLINGSPERIODER_ELLER_ORGTILKNYTNINGER));
    }

    @Test
    @DisplayName("Upresis AgressoId matcher IKKE presis AgressoId på Ressurs - bruk evt OT som matcher upresis agressoId - Ref: 2.ii.a")
    void test_UpresisAgressoIdMatcherIkkePresisRessursAgressoId_BrukOT_2_ii_a() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("02200", "0110001", "Nytt navn", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.matchByAgressoId("02200")).thenReturn(List.of(
            orgenhet("x123yz", "Annen enhet",           "0220002", LocalDate.of(2022, 1, 1), null)
        ));

        when(orgenhetService.getById("x123yz")).thenReturn(Optional.of(
            orgenhet("x123yz", "Annen enhet",           "0220002", LocalDate.of(2022, 1, 1), null)
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "x123yz", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 1, 31), false)
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31),forsteStillingPeriode.gyldigTom());
        assertEquals("x123yz",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(AGRESSOID_PAA_STILLING_ER_IKKE_PRESIS_MEN_BRUKER_EKSISTERENDE_OT_SOM_MATCHER));
    }

    @Test
    @DisplayName("Upresis AgressoId matcher IKKE presis AgressoId på Ressurs - flere OT matcher upresis agressoId - bruk den som er DOL - Ref: 2.ii.b")
    void test_UpresisAgressoIdMatcherIkkePresisRessursAgressoId_FlereOTMatcher_BrukDOL_2_ii_b() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("02200", "0110001", "Nytt navn", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.matchByAgressoId("02200")).thenReturn(List.of(
            orgenhet("x123yz", "Annen enhet",           "0220002", LocalDate.of(2022, 1, 1), null)
        ));

        when(orgenhetService.getById("x123yz")).thenReturn(Optional.of(
            orgenhet("x123yz", "Annen enhet",           "0220002", LocalDate.of(2022, 1, 1), null)
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "x123yz", LocalDate.of(2022, 1, 1), LocalDate.of(2023, 1, 31), false),
            orgtilknytning(A000001, "x123yz", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 1, 31), true)
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1, 31),forsteStillingPeriode.gyldigTom());
        assertEquals("x123yz",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(AGRESSOID_PAA_STILLING_ER_IKKE_PRESIS_MEN_BRUKER_EKSISTERENDE_OT_SOM_MATCHER));
    }

    @Test
    @DisplayName("Upresis AgressoId matcher IKKE presis AgressoId på Ressurs - flere OT matcher upresis agressoId - ingen DOL - bruk den første - Ref: 2.ii.c")
    void test_UpresisAgressoIdMatcherIkkePresisRessursAgressoId_FlereOTMatcher_IngenDOL_brukFoerste_2_ii_c() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("02200", "0110001", "Nytt navn", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.matchByAgressoId("02200")).thenReturn(List.of(
            orgenhet("x123yz", "Annen enhet",           "0220002", LocalDate.of(2022, 1, 1), null)
        ));

        when(orgenhetService.getById("x123yz")).thenReturn(Optional.of(
            orgenhet("x123yz", "Annen enhet",           "0220002", LocalDate.of(2022, 1, 1), null)
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "x123yz", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 31), false),
            orgtilknytning(A000001, "x123yz", LocalDate.of(2022, 1, 1), LocalDate.of(2023, 1, 31), false)
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2022, 1, 31),forsteStillingPeriode.gyldigTom());
        assertEquals("x123yz",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(AGRESSOID_PAA_STILLING_ER_IKKE_PRESIS_MEN_BRUKER_EKSISTERENDE_OT_SOM_MATCHER));
    }


    @Test
    @DisplayName("Upresis AgressoId matcher IKKE presis AgressoId på Ressurs - flere OT matcher upresis agressoId - ingen DOL - bruk den som er current - Ref: 2.ii.d")
    void test_UpresisAgressoIdMatcherIkkePresisRessursAgressoId_FlereOTMatcher_IngenDOL_brukCurrent_2_ii_d() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("02200", "0110001", "Nytt navn", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.matchByAgressoId("02200")).thenReturn(List.of(
            orgenhet("x123yz", "Annen enhet",           "0220002", LocalDate.of(2022, 1, 1), null)
        ));

        when(orgenhetService.getById("x123yz")).thenReturn(Optional.of(
            orgenhet("x123yz", "Annen enhet",           "0220002", LocalDate.of(2022, 1, 1), null)
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "x123yz", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 31), false),
            orgtilknytning(A000001, "x123yz", LocalDate.of(2023, 1, 1), today().plusYears(1),      false)
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2023, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(today().plusYears(1),      forsteStillingPeriode.gyldigTom());
        assertEquals("x123yz",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(AGRESSOID_PAA_STILLING_ER_IKKE_PRESIS_MEN_BRUKER_EKSISTERENDE_OT_SOM_MATCHER));
    }


    @Test
    @DisplayName("Upresis AgressoId matcher IKKE presis AgressoId på Ressurs - matcher en OT i delperiode - Ref: 2.ii.e")
    void test_UpresisAgressoIdMatcherIkkePresisRessursAgressoId_MatcherOTiDelperiode_2_ii_e() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("02200", "0110001", "Nytt navn", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 1, 31))
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.matchByAgressoId("02200")).thenReturn(List.of(
            orgenhet("x123yz", "Annen enhet",           "0220002", LocalDate.of(2022, 1, 1), null)
        ));

        when(orgenhetService.getById("x123yz")).thenReturn(Optional.of(
            orgenhet("x123yz", "Annen enhet",           "0220002", LocalDate.of(2022, 1, 1), null)
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "x123yz", LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 31), false)
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2023, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(LocalDate.of(2024, 1,31),  forsteStillingPeriode.gyldigTom());
        assertEquals("x123yz",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(AGRESSOID_PAA_STILLING_ER_IKKE_PRESIS_MEN_BRUKER_EKSISTERENDE_OT_SOM_MATCHER));
    }


    @Test
    @DisplayName("Upresis AgressoId matcher IKKE presis AgressoId på Ressurs " +
        "-stillingPerioden er current - matche på presis ressursAgressoId - Ref: 2.iii.a")
    void test_UpresisAgressoIdMatcherIkkePresisRessursAgressoId_CurrentStillingPeriode_MatchPaaPresisRessursAgressoId_2_iii_a() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("02200", "0110001", "Nytt navn", LocalDate.of(2022, 1, 1), NO_END_DATE)
        );

        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        when(orgenhetService.exactMatchByAgressoId("0110001")).thenReturn(List.of(
            orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), null)
        ));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2022, 1, 1), null, false)
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(NO_END_DATE,               forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(0, result.errors().size());
    }


    @Test
    @DisplayName("Upresis AgressoId og upresis AgressoId på Ressurs " +
        "- stillingPerioden er current - matche på upresis agressoId - Ref: 3.i")
    void test_UpresisAgressoIdOgUpresisRessursAgressoId_CurrentStillingPeriode_MatchPaaUpresisAgressoId_3_i() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("02200", "01100", "Nytt navn", LocalDate.of(2022, 1, 1), NO_END_DATE)
        );

        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        var x123yz = orgenhet("x123yz", "Annen enhet", "0220002", LocalDate.of(2022, 1, 1), null);

        when(orgenhetService.matchByAgressoId("02200")).thenReturn(List.of(x123yz));
        when(orgenhetService.getById("x123yz")).thenReturn(Optional.of(x123yz));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "x123yz", LocalDate.of(2023, 1, 1), null, false)
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2023, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(NO_END_DATE,               forsteStillingPeriode.gyldigTom());
        assertEquals("x123yz",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(AGRESSOID_PAA_STILLING_ER_IKKE_PRESIS_MEN_BRUKER_EKSISTERENDE_OT_SOM_MATCHER));
    }

    @Test
    @DisplayName("Upresis AgressoId og upresis AgressoId på Ressurs " +
        "- stillingPerioden er current - ingen OT for agressoID - matche på upresis ressursAgressoId - Ref: 3.ii")
    void test_UpresisAgressoIdOgUpresisRessursAgressoId_CurrentStillingPeriode_IngenOtForAgressoId_MatchPaaUpresisRessursAgressoId_3_ii() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("02200", "01100", "Nytt navn", LocalDate.of(2022, 1, 1), NO_END_DATE)
        );

        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        var x123yz = orgenhet("x123yz", "Annen enhet",   "0220002", LocalDate.of(2022, 1, 1), null);
        var a123bc = orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), null);

        when(orgenhetService.matchByAgressoId("02200")).thenReturn(List.of(x123yz));
        when(orgenhetService.getById("x123yz")).thenReturn(Optional.of(x123yz));

        when(orgenhetService.matchByAgressoId("01100")).thenReturn(List.of(a123bc));
        when(orgenhetService.getById("a123bc")).thenReturn(Optional.of(a123bc));

        when(orgtilknytningStateRepository.findByNavident(A000001)).thenReturn(List.of(
            orgtilknytning(A000001, "a123bc", LocalDate.of(2022, 1, 1), null, false)
        ));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(1, result.stillingPerioder().size());
        var forsteStillingPeriode = result.stillingPerioder().getFirst();
        assertEquals(LocalDate.of(2022, 1, 1),  forsteStillingPeriode.gyldigFom());
        assertEquals(NO_END_DATE,               forsteStillingPeriode.gyldigTom());
        assertEquals("a123bc",                  forsteStillingPeriode.orgenhet().getId());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(AGRESSOID_PAA_STILLING_ER_IKKE_PRESIS_MEN_BRUKER_EKSISTERENDE_OT_SOM_MATCHER));
    }

    @Test
    @DisplayName("Upresis AgressoId og upresis AgressoId på Ressurs " +
        "- stillingPerioden er current - ingen OT match - Ref: 3.iii")
    void test_UpresisAgressoIdOgUpresisRessursAgressoId_CurrentStillingPeriode_IngenOtMatch_3_iii() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("02200", "01100", "Nytt navn", LocalDate.of(2022, 1, 1), NO_END_DATE)
        );

        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        var x123yz = orgenhet("x123yz", "Annen enhet",   "0220002", LocalDate.of(2022, 1, 1), null);
        var a123bc = orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), null);

        when(orgenhetService.matchByAgressoId("02200")).thenReturn(List.of(x123yz));
        when(orgenhetService.getById("x123yz")).thenReturn(Optional.of(x123yz));

        when(orgenhetService.matchByAgressoId("01100")).thenReturn(List.of(a123bc));
        when(orgenhetService.getById("a123bc")).thenReturn(Optional.of(a123bc));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(0, result.stillingPerioder().size());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(KAN_IKKE_MATCHE_STILLINGSPERIODE_TIL_ENHET));
    }

    @Test
    @DisplayName("Null AgressoId og upresis AgressoId på Ressurs " +
        "- stillingPerioden er current - ingen OT match - Ref: 3.iii")
    void test_NullAgressoIdOgUpresisRessursAgressoId_CurrentStillingPeriode_IngenOtMatch_3_iii() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode(null, "01100", "Nytt navn", LocalDate.of(2022, 1, 1), NO_END_DATE)
        );

        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        var x123yz = orgenhet("x123yz", "Annen enhet",   "0220002", LocalDate.of(2022, 1, 1), null);
        var a123bc = orgenhet("a123bc", "Gammelt navn",  "0110001", LocalDate.of(2022, 1, 1), null);

        when(orgenhetService.matchByAgressoId("02200")).thenReturn(List.of(x123yz));
        when(orgenhetService.getById("x123yz")).thenReturn(Optional.of(x123yz));

        when(orgenhetService.matchByAgressoId("01100")).thenReturn(List.of(a123bc));
        when(orgenhetService.getById("a123bc")).thenReturn(Optional.of(a123bc));

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(0, result.stillingPerioder().size());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(KAN_IKKE_MATCHE_STILLINGSPERIODE_TIL_ENHET));
    }

    @Test
    @DisplayName("Feil AgressoId og feil AgressoId på Ressurs - Ref: 4")
    void testFeilAgressoIdOgRessursAgressoId_4() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("-4", "-3", "Mangler", LocalDate.of(2022, 1, 1), NO_END_DATE)
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(0, result.stillingPerioder().size());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(KAN_IKKE_MATCHE_STILLINGSPERIODE_TIL_ENHET));
    }

    @Test
    @DisplayName("Null AgressoId og AgressoId på Ressurs - Ref: 4")
    void testNullAgressoIdOgRessursAgressoId_4() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode(null, null, "Mangler", LocalDate.of(2022, 1, 1), NO_END_DATE)
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(0, result.stillingPerioder().size());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(KAN_IKKE_MATCHE_STILLINGSPERIODE_TIL_ENHET));
    }

    @Test
    @DisplayName("Tom AgressoId og AgressoId på Ressurs - Ref: 4")
    void testTomAgressoIdOgRessursAgressoId_4() {
        var strukturerteStillingPerioder = new StrukturerteStillingPerioderForNavident(A000001, Collections.emptyList());

        List<StillingPeriode> stillingperioder =  List.of(
            stillingPeriode("", "", "Mangler", LocalDate.of(2022, 1, 1), NO_END_DATE)
        );
        strukturerteStillingPerioder.addStillingPerioder(stillingperioder);

        var result = CUT.behandle(strukturerteStillingPerioder);

        assertEquals(0, result.stillingPerioder().size());

        assertEquals(1, result.errors().size());
        assertEquals(1, result.countOfErrorType(KAN_IKKE_MATCHE_STILLINGSPERIODE_TIL_ENHET));
    }
}