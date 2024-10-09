package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode.DagligOppfolging.JA;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode.DagligOppfolging.NEI;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode.DagligOppfolging.UKJENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Tester util metodene som bruker i strukturering av periodisering på RessursStillinger fra DVH")
public class UtilTest extends TestBase {

    StrukturerteStillingPerioderForNavident DUMMY_sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", new ArrayList<>(), new ArrayList<>());

    @Test
    @DisplayName("Test slå sammen sammenhengende Stillingperioder for samme enhet og DOL - hull i periode - ingen å slå sammen")
    void slaaSammenStillingPerioder_hullIPeriode() {

        IOrgenhet orgenhet = orgenhet("a123bc", "Enhet 1", "1100001");
        List<StillingPeriode> stillingPerioder = List.of(
            new StillingPeriode(null, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31), orgenhet, JA), //
            new StillingPeriode(null, LocalDate.of(2020, 5, 1), LocalDate.of(2021, 10, 31), orgenhet, JA) //
        );

        var result = Util.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

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

        var result = Util.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

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

        var result = Util.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

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

        var result = Util.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

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

        var result = Util.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

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

        var result = Util.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

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

        var result = Util.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

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

        var result = Util.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

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

        var result = Util.slaaSammenBackToBackPerioderOgSorterAsc(stillingPerioder, DUMMY_sspForNavident);

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
