package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode.DagligOppfolging;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("Tester gruppering og strukturering av periodisering på RessursStillinger fra DVH")
class RessursStillingStrukturererTest extends TestBase {
    private static final LocalDate MAX_DATE = LocalDate.of(2099, 12, 31);

    @Autowired
    private RessursStillingStrukturerer ressursStillingStrukturerer;

    @MockBean
    OrgenhetService orgenhetService;

    @MockBean
    RessursStateRepository ressursStateRepository;

    private static IOrgenhet dummyEnhet_1, dummyEnhet_2, dummyEnhet_3, dummyEnhet_4;
    private static String navident_A001001 = "A001001";
    private static String navident_B002002 = "B002002";

    @BeforeAll
    public static void init() {
        dummyEnhet_1 = getNomEnhet("FID_1", "x123yz", "Enhet_1", LocalDate.of(2000, 1, 1), null, "AGRID_1");
        dummyEnhet_2 = getNomEnhet("FID_2", "z321xy", "Enhet_2", LocalDate.of(2000, 1, 1), null, "AGRID_2");
        dummyEnhet_3 = getNomEnhet("FID_3", "a123bc", "Enhet_3", LocalDate.of(2000, 1, 1), null, "AGRID_3");
        dummyEnhet_4 = getNomEnhet("FID_4", "a321bc", "Enhet_4", LocalDate.of(2000, 1, 1), null, "AGRID_4");
    }

    @BeforeEach
    public void beforeEach() {
        when(ressursStateRepository.findByNavident(navident_A001001))
            .thenReturn(Optional.of(new RessursState("FID_1",
                "01010154321",
                navident_A001001,
                Sektor.NAV_STATLIG,
                null,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                null,
                Collections.emptyList(),
                IdentType.STANDARD
            )));
        when(ressursStateRepository.findByNavident(navident_B002002))
            .thenReturn(Optional.of(new RessursState(
                "FID_2",
                "02020254321",
                navident_B002002,
                Sektor.NAV_STATLIG,
                null,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                null,
                Collections.emptyList(),
                IdentType.STANDARD
            )));
    }

    @NotNull
    private static OrgEnhet getNomEnhet(String fid, String id, String navn, LocalDate fra, LocalDate til, String agressoId) {
        return new OrgEnhet(new OrgEnhetState(
            fid,
            id,
            navn,
            fra,
            Optional.ofNullable(til),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(agressoId + "-ORGENHET-" + navn),
            Optional.of(agressoId),
            Optional.of("ORGENHET"),
            Optional.empty()
        ));
    }

    @Test
    @DisplayName("Test Gruppering per navident av RessursStillinger")
    public void test_001() {
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE),
            getTestRessursStilling(navident_B002002, "AGRID_2", "Enhet_2", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navident_B002002, "AGRID_2", "Enhet_2", LocalDate.of(2020, 1, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navident_B002002, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        var result = ressursStillingStrukturerer.grupperPerNavident(ressursStillinger);
        assertEquals(2, result.size());
        assertEquals(3, result.get(navident_A001001).size());
        assertEquals(3, result.get(navident_B002002).size());
    }

    @Test
    @DisplayName("Test Gruppering per navident av RessursStillinger - annen sortering")
    public void test_002() {
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navident_B002002, "AGRID_2", "Enhet_2", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navident_B002002, "AGRID_2", "Enhet_2", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE),
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navident_B002002, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        var result = ressursStillingStrukturerer.grupperPerNavident(ressursStillinger);
        assertEquals(2, result.size());
        assertEquals(3, result.get(navident_A001001).size());
        assertEquals(3, result.get(navident_B002002).size());
    }

    @Test
    @DisplayName("Test strukturer per navident av RessursStillinger")
    public void test_003() {
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );


        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));
        var result = ressursStillingStrukturerer.strukturerNavidentensRessursStillinger(navident_A001001, ressursStillinger);
        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2020, 1, 1), result.get(0).gyldigFom());
        assertEquals(MAX_DATE, result.get(0).gyldigTom());
    }

    @Test
    @DisplayName("Test strukturer per navident av RessursStillinger - Splitt på ikke sammenhengende periode")
    public void test_004() {
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 8, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1));
        var result = ressursStillingStrukturerer.strukturerNavidentensRessursStillinger(navident_A001001, ressursStillinger);
        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2020, 1, 1), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2020, 8, 31), result.get(0).gyldigTom());
        assertEquals(LocalDate.of(2020, 11, 1), result.get(1).gyldigFom());
        assertEquals(MAX_DATE, result.get(1).gyldigTom());
    }

    @Test
    @DisplayName("Test strukturer per navident av RessursStillinger - Splitt på ulike enheter")
    public void test_005() {
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_2", "Enhet_2", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));
        var result = ressursStillingStrukturerer.strukturerNavidentensRessursStillinger(navident_A001001, ressursStillinger);
        assertEquals(2, result.size());
        assertEquals("AGRID_1", result.get(0).stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 1, 1), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2020, 10, 31), result.get(0).gyldigTom());
        assertEquals("AGRID_2", result.get(1).stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 11, 1), result.get(1).gyldigFom());
        assertEquals(MAX_DATE, result.get(1).gyldigTom());
    }

    @Test
    @DisplayName("Test strukturer per navident av RessursStillinger - Splitt på ulike enheter - Skip overlapp")
    public void test_006() {
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", LocalDate.of(2020, 3, 1), MAX_DATE),
            getTestRessursStilling(navident_A001001, "AGRID_2", "Enhet_2", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));
        var result = ressursStillingStrukturerer.strukturerNavidentensRessursStillinger(navident_A001001, ressursStillinger);
        assertEquals(2, result.size());
        assertEquals("AGRID_1", result.get(0).stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 1, 1), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2020, 10, 31), result.get(0).gyldigTom());
        assertEquals("AGRID_2", result.get(1).stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 11, 1), result.get(1).gyldigFom());
        assertEquals(MAX_DATE, result.get(1).gyldigTom());
    }

    @Test
    @DisplayName("Test strukturer en dvh stilling hvor agressoId spenner over 2 nom enheter som er splittet på periode")
    public void test_007() {
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navident_A001001, "AGRID_1", "SAMME NAVN", LocalDate.of(2020, 1, 1), MAX_DATE)
        );

        List<IOrgenhet> agrid_1_enheter = List.of(
            getNomEnhet("FID_1", "a123bc", "SAMME NAVN", LocalDate.of(2000, 1, 1), LocalDate.of(2020, 5, 31), "AGRID_1"),
            getNomEnhet("FID_2", "x987yz", "SAMME NAVN", LocalDate.of(2000, 6, 1), null, "AGRID_1")
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(agrid_1_enheter);

        var result = ressursStillingStrukturerer.strukturerNavidentensRessursStillinger(navident_A001001, ressursStillinger);
        assertEquals(2, result.size());
        assertEquals("AGRID_1", result.get(0).stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 1, 1), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2020, 5, 31), result.get(0).gyldigTom());
        assertEquals("FID_1", result.get(0).orgenhet().getFid());
        assertEquals("AGRID_1", result.get(1).stilling().ressursAgressoId().orElse("NULL"));
        assertEquals(LocalDate.of(2020, 6, 1), result.get(1).gyldigFom());
        assertEquals(MAX_DATE, result.get(1).gyldigTom());
        assertEquals("FID_2", result.get(1).orgenhet().getFid());
    }

    @Test
    @DisplayName("Test å strukturere en aktiv dvh stilling hvor stillingens agressoId er upresis (ikke ORGENHET) men ressursens agressoId er")
    public void test_008() {
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navident_A001001, "AGRID", "Enhet_1", "AGRID_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1));
        var result = ressursStillingStrukturerer.strukturerNavidentensRessursStillinger(navident_A001001, ressursStillinger);
        assertEquals(1, result.size());
        assertEquals("AGRID_1", result.get(0).stilling().ressursAgressoId().orElse("NULL"));
        assertEquals("FID_1", result.get(0).orgenhet().getFid());
    }

    @Test
    @DisplayName("Test å strukturere en aktiv dvh stilling hvor stillingen periode går ut over periode til kjent Orgenhet")
    public void test_009() {
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navident_A001001, "AGRID", "Enhet_1", "AGRID_1", LocalDate.of(2018, 1, 1), MAX_DATE)
        );

        IOrgenhet enhet = getNomEnhet("FID_1", "a123bc", "Enhet_1", LocalDate.of(2020, 1, 1), null, "AGRID_1");

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(enhet));

        var result = ressursStillingStrukturerer.strukturerNavidentensRessursStillinger(navident_A001001, ressursStillinger);
        assertEquals(1, result.size());
        assertEquals("AGRID_1", result.get(0).stilling().ressursAgressoId().orElse("NULL"));
        assertEquals("FID_1", result.get(0).orgenhet().getFid());
        assertEquals(LocalDate.of(2020, 1, 1), result.get(0).gyldigFom()); // Strukturert stilling skal kun tas med fra enhet start dato
    }

    @Test
    @DisplayName(
        "Test å strukturere en aktiv dvh stilling hvor stillingen periode feilaktige Orgenheter som er like med overlapp på dato (dette er en feil i orgenhet i såfall)"
    )
    // Dette vil gå igjennom, men blir error melding på enhets feil, og kobling blir gjort mot første treff
    public void test_010() {
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navident_A001001, "AGRID", "Enhet_1", "AGRID_1", LocalDate.of(2020, 1, 1), MAX_DATE)
        );

        IOrgenhet enhet = getNomEnhet("FID_1", "a123bc", "Enhet_1", LocalDate.of(2018, 1, 1), null, "AGRID_1");
        IOrgenhet enhet2 = getNomEnhet("FID_2", "x321yz", "Enhet_1", LocalDate.of(2018, 1, 1), null, "AGRID_1");

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(enhet, enhet2));

        var result = ressursStillingStrukturerer.strukturerNavidentensRessursStillinger(navident_A001001, ressursStillinger);
        assertEquals(1, result.size());
        assertEquals("AGRID_1", result.get(0).stilling().ressursAgressoId().orElse("NULL"));
        assertEquals("FID_1", result.get(0).orgenhet().getFid());
        assertEquals(LocalDate.of(2020, 1, 1), result.get(0).gyldigFom()); // Strukturert stilling skal kun tas med fra enhet start dato
    }

    @Test
    @DisplayName("Test strukturer per navident av RessursStillinger hvor DVH stillinger fortsetter etter ressursens sluttdato")
    public void test_011() {
        String navid_3 = "C003003";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_3, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_3, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2022, 12, 31)),
            getTestRessursStilling(navid_3, "AGRID_1", "Enhet_1", LocalDate.of(2023, 1, 1), MAX_DATE)
        );

        LocalDate ressursSluttdato = LocalDate.of(2022, 1, 31);

        when(ressursStateRepository.findByNavident(navid_3))
            .thenReturn(Optional.of(new RessursState(
                "FID_3",
                "03030354321",
                navid_3,
                Sektor.NAV_STATLIG,
                null,
                Optional.empty(),
                Optional.of(ressursSluttdato),
                Optional.empty(),
                null,
                Collections.emptyList(),
                IdentType.STANDARD
            )));

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));

        var result = ressursStillingStrukturerer.strukturerNavidentensRessursStillinger(navid_3, ressursStillinger);
        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2020, 1, 1), result.get(0).gyldigFom());
        assertEquals(ressursSluttdato, result.get(0).gyldigTom());
    }

    @Test
    @DisplayName("Test splitt på perioder, og angivelse av DagligOppfølging")
    public void test_012() {
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", "AGRID_?", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 8, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", "AGRID_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", "AGRID_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));
        var result = ressursStillingStrukturerer.strukturerStilingPerioderPerNavident(ressursStillinger);
        assertEquals(1, result.size());

        var stillingPerioderFor_A001001 = result.get(navident_A001001);
        assertEquals(2, stillingPerioderFor_A001001.size());

        var stillingPeriode_1 = stillingPerioderFor_A001001.get(0);
        var stillingPeriode_2 = stillingPerioderFor_A001001.get(1);

        assertEquals(LocalDate.of(2020, 1, 1), stillingPeriode_1.gyldigFom());
        assertEquals(LocalDate.of(2020, 8, 31), stillingPeriode_1.gyldigTom());
        assertEquals(DagligOppfolging.JA, stillingPeriode_1.dagligOppfolging());

        assertEquals(LocalDate.of(2020, 11, 1), stillingPeriode_2.gyldigFom());
        assertEquals(MAX_DATE, stillingPeriode_2.gyldigTom());
        assertEquals(DagligOppfolging.JA, stillingPeriode_2.dagligOppfolging());
    }

    @Test
    @DisplayName("Test splitt på perioder, og angivelse av DagligOppfølging - med overlapp historisk")
    public void test_013() {
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", "AGRID_1", LocalDate.of(2020, 1, 1), LocalDate.of(2022, 8, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_2", "Enhet_2", "AGRID_2", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navident_A001001, "AGRID_2", "Enhet_2", "AGRID_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));
        var result = ressursStillingStrukturerer.strukturerStilingPerioderPerNavident(ressursStillinger);
        assertEquals(1, result.size());

        var stillingPerioderFor_A001001 = result.get(navident_A001001);
        assertEquals(4, stillingPerioderFor_A001001.size());

        var stillingPeriode_1 = stillingPerioderFor_A001001.get(0);
        var stillingPeriode_2 = stillingPerioderFor_A001001.get(1);
        var stillingPeriode_3 = stillingPerioderFor_A001001.get(2);
        var stillingPeriode_4 = stillingPerioderFor_A001001.get(3);

        assertEquals(LocalDate.of(2020, 1, 1), stillingPeriode_1.gyldigFom());
        assertEquals(LocalDate.of(2020, 10, 31), stillingPeriode_1.gyldigTom());
        assertEquals(DagligOppfolging.JA, stillingPeriode_1.dagligOppfolging());

        assertEquals(LocalDate.of(2020, 11, 1), stillingPeriode_2.gyldigFom());
        assertEquals(LocalDate.of(2022, 8, 31), stillingPeriode_2.gyldigTom());
        assertEquals(DagligOppfolging.UKJENT, stillingPeriode_2.dagligOppfolging());

        assertEquals(LocalDate.of(2020, 11, 1), stillingPeriode_3.gyldigFom());
        assertEquals(LocalDate.of(2022, 8, 31), stillingPeriode_3.gyldigTom());
        assertEquals(DagligOppfolging.UKJENT, stillingPeriode_3.dagligOppfolging());

        assertEquals(LocalDate.of(2022, 9, 1), stillingPeriode_4.gyldigFom());
        assertEquals(MAX_DATE, stillingPeriode_4.gyldigTom());
        assertEquals(DagligOppfolging.JA, stillingPeriode_4.dagligOppfolging());
    }

    @Test
    @DisplayName("Test splitt på perioder, og angivelse av DagligOppfølging - med overlapp på dagens dato. Velg agressoId likhet")
    public void test_014() {
        LocalDate endOfThreeMonthsAfterNow = LocalDate.now().plusMonths(3).with(lastDayOfMonth());
        LocalDate startOfThreeMonthBeforeNow = LocalDate.now().minusMonths(3).with(firstDayOfMonth());
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", "AGRID_2", LocalDate.of(2020, 1, 1), endOfThreeMonthsAfterNow),
            getTestRessursStilling(navident_A001001, "AGRID_2", "Enhet_2", "AGRID_2", startOfThreeMonthBeforeNow, MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));
        var result = ressursStillingStrukturerer.strukturerStilingPerioderPerNavident(ressursStillinger);
        assertEquals(1, result.size());

        var stillingPerioderFor_A001001 = result.get(navident_A001001);
        assertEquals(4, stillingPerioderFor_A001001.size());

        var stillingPeriode_1 = stillingPerioderFor_A001001.get(0);
        var stillingPeriode_2 = stillingPerioderFor_A001001.get(1);
        var stillingPeriode_3 = stillingPerioderFor_A001001.get(2);
        var stillingPeriode_4 = stillingPerioderFor_A001001.get(3);

        assertEquals(LocalDate.of(2020, 1, 1), stillingPeriode_1.gyldigFom());
        assertEquals(startOfThreeMonthBeforeNow.minusDays(1), stillingPeriode_1.gyldigTom());
        assertEquals(DagligOppfolging.JA, stillingPeriode_1.dagligOppfolging());

        assertEquals(startOfThreeMonthBeforeNow, stillingPeriode_2.gyldigFom());
        assertEquals(endOfThreeMonthsAfterNow, stillingPeriode_2.gyldigTom());
        assertEquals(DagligOppfolging.NEI, stillingPeriode_2.dagligOppfolging());

        assertEquals(startOfThreeMonthBeforeNow, stillingPeriode_3.gyldigFom());
        assertEquals(endOfThreeMonthsAfterNow, stillingPeriode_3.gyldigTom());
        assertEquals(DagligOppfolging.JA, stillingPeriode_3.dagligOppfolging());

        assertEquals(endOfThreeMonthsAfterNow.plusDays(1), stillingPeriode_4.gyldigFom());
        assertEquals(MAX_DATE, stillingPeriode_4.gyldigTom());
        assertEquals(DagligOppfolging.JA, stillingPeriode_4.dagligOppfolging());
    }

    @Test
    @DisplayName("Test splitt på perioder, og angivelse av DagligOppfølging - med full overlapp samme start/slutt")
    public void test_015() {
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navident_A001001, "AGRID_1", "Enhet_1", "AGRID_1", LocalDate.of(2020, 1, 1), MAX_DATE),
            getTestRessursStilling(navident_A001001, "AGRID_2", "Enhet_2", "AGRID_1", LocalDate.of(2020, 1, 1), MAX_DATE),
            getTestRessursStilling(navident_A001001, "AGRID_3", "Enhet_3", "AGRID_1", LocalDate.of(2020, 10, 1), MAX_DATE),
            getTestRessursStilling(navident_A001001, "AGRID_4", "Enhet_4", "AGRID_1", LocalDate.of(2020, 10, 1), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2, dummyEnhet_3, dummyEnhet_4));
        var result = ressursStillingStrukturerer.strukturerStilingPerioderPerNavident(ressursStillinger);
        assertEquals(1, result.size());

        var stillingPerioderFor_A001001 = result.get(navident_A001001);
        assertEquals(6, stillingPerioderFor_A001001.size());

        var stillingPeriode_1 = stillingPerioderFor_A001001.get(0);
        var stillingPeriode_2 = stillingPerioderFor_A001001.get(1);
        var stillingPeriode_3 = stillingPerioderFor_A001001.get(2);
        var stillingPeriode_4 = stillingPerioderFor_A001001.get(3);
        var stillingPeriode_5 = stillingPerioderFor_A001001.get(4);
        var stillingPeriode_6 = stillingPerioderFor_A001001.get(5);

        assertEquals(LocalDate.of(2020, 1, 1), stillingPeriode_1.gyldigFom());
        assertEquals(LocalDate.of(2020, 9, 30), stillingPeriode_1.gyldigTom());
        assertEquals(DagligOppfolging.UKJENT, stillingPeriode_1.dagligOppfolging());

        assertEquals(LocalDate.of(2020, 1, 1), stillingPeriode_2.gyldigFom());
        assertEquals(LocalDate.of(2020, 9, 30), stillingPeriode_2.gyldigTom());
        assertEquals(DagligOppfolging.UKJENT, stillingPeriode_2.dagligOppfolging());

        assertEquals(LocalDate.of(2020, 10, 1), stillingPeriode_3.gyldigFom());
        assertEquals(MAX_DATE, stillingPeriode_3.gyldigTom());
        assertEquals(DagligOppfolging.JA, stillingPeriode_3.dagligOppfolging());

        assertEquals(LocalDate.of(2020, 10, 1), stillingPeriode_4.gyldigFom());
        assertEquals(MAX_DATE, stillingPeriode_4.gyldigTom());
        assertEquals(DagligOppfolging.NEI, stillingPeriode_4.dagligOppfolging());

        assertEquals(LocalDate.of(2020, 10, 1), stillingPeriode_5.gyldigFom());
        assertEquals(MAX_DATE, stillingPeriode_5.gyldigTom());
        assertEquals(DagligOppfolging.NEI, stillingPeriode_5.dagligOppfolging());

        assertEquals(LocalDate.of(2020, 10, 1), stillingPeriode_6.gyldigFom());
        assertEquals(MAX_DATE, stillingPeriode_6.gyldigTom());
        assertEquals(DagligOppfolging.NEI, stillingPeriode_6.dagligOppfolging());
    }

    @Test
    @DisplayName("Test når aktiv OT skal bli splittet fordi det lages ny Enhet med samme AgressoID, og de splittes i OT sin periode")
    public void test_016() {
        var dummyEnhet_5a = getNomEnhet("FID_5a", "q321we", "Enhet_5a", LocalDate.of(2000, 1, 1), LocalDate.now().minusMonths(1), "AGRID_5");
        var dummyEnhet_5b = getNomEnhet("FID_5b", "q123we", "Enhet_5b", LocalDate.now().minusMonths(1).plusDays(1), null, "AGRID_5");

        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navident_A001001, "AGRID_5", "Enhet_5b", "AGRID_5", LocalDate.now().minusYears(1), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_5a, dummyEnhet_5b));
        var result = ressursStillingStrukturerer.strukturerStilingPerioderPerNavident(ressursStillinger);
//        assertEquals(1, result.size());
//
//        var stillingPerioderFor_A001001 = result.get(navident_A001001);
//        assertEquals(6, stillingPerioderFor_A001001.size());
//
//        var stillingPeriode_1 = stillingPerioderFor_A001001.get(0);
//        var stillingPeriode_2 = stillingPerioderFor_A001001.get(1);
//        var stillingPeriode_3 = stillingPerioderFor_A001001.get(2);
//        var stillingPeriode_4 = stillingPerioderFor_A001001.get(3);
//        var stillingPeriode_5 = stillingPerioderFor_A001001.get(4);
//        var stillingPeriode_6 = stillingPerioderFor_A001001.get(5);
//
//        assertEquals(LocalDate.of(2020, 1, 1), stillingPeriode_1.gyldigFom());
//        assertEquals(LocalDate.of(2020, 9, 30), stillingPeriode_1.gyldigTom());
//        assertEquals(DagligOppfolging.UKJENT, stillingPeriode_1.dagligOppfolging());
//
//        assertEquals(LocalDate.of(2020, 1, 1), stillingPeriode_2.gyldigFom());
//        assertEquals(LocalDate.of(2020, 9, 30), stillingPeriode_2.gyldigTom());
//        assertEquals(DagligOppfolging.UKJENT, stillingPeriode_2.dagligOppfolging());
//
//        assertEquals(LocalDate.of(2020, 10, 1), stillingPeriode_3.gyldigFom());
//        assertEquals(MAX_DATE, stillingPeriode_3.gyldigTom());
//        assertEquals(DagligOppfolging.JA, stillingPeriode_3.dagligOppfolging());
//
//        assertEquals(LocalDate.of(2020, 10, 1), stillingPeriode_4.gyldigFom());
//        assertEquals(MAX_DATE, stillingPeriode_4.gyldigTom());
//        assertEquals(DagligOppfolging.NEI, stillingPeriode_4.dagligOppfolging());
//
//        assertEquals(LocalDate.of(2020, 10, 1), stillingPeriode_5.gyldigFom());
//        assertEquals(MAX_DATE, stillingPeriode_5.gyldigTom());
//        assertEquals(DagligOppfolging.NEI, stillingPeriode_5.dagligOppfolging());
//
//        assertEquals(LocalDate.of(2020, 10, 1), stillingPeriode_6.gyldigFom());
//        assertEquals(MAX_DATE, stillingPeriode_6.gyldigTom());
//        assertEquals(DagligOppfolging.NEI, stillingPeriode_6.dagligOppfolging());
    }


    @Test
    @DisplayName("Test sorting orgenheter by gyldigTom desc")
    public void testSortOrgenheterDesc(){
        IOrgenhet enhet1 = getNomEnhet("FID_1", "a123bc", "Enhet_1", LocalDate.of(2013, 1, 1),  LocalDate.of(2019, 1, 1), "AGRID_1");
        IOrgenhet enhet2 = getNomEnhet("FID_2", "b123bc", "Enhet_2", LocalDate.of(2012, 1, 1),  LocalDate.of(2018, 1, 1), "AGRID_2");
        IOrgenhet enhet3 = getNomEnhet("FID_3", "c123bc", "Enhet_3", LocalDate.of(2014, 1, 1),  LocalDate.of(2019, 1, 1), "AGRID_3");
        IOrgenhet enhet4 = getNomEnhet("FID_4", "d123bc", "Enhet_4", LocalDate.of(2016, 1, 1),  LocalDate.of(2020, 1, 1), "AGRID_4");

        List<IOrgenhet> tidligereNomEnheter = new ArrayList<>();
        Collections.addAll(tidligereNomEnheter, enhet1, enhet2, enhet3, enhet4);
        assertEquals(tidligereNomEnheter.size(), 4);

        List<IOrgenhet> tidligereNomEnheterReverseSorted_expected = new ArrayList<>();
        Collections.addAll(tidligereNomEnheterReverseSorted_expected, enhet4, enhet3, enhet1, enhet2);

        List<IOrgenhet> tidligereNomEnheterReverseSorted = ressursStillingStrukturerer.reverseSortNomEnheterByGyldigTom(tidligereNomEnheter);
        assertEquals(tidligereNomEnheterReverseSorted.size(), 4);
        assertNotEquals(tidligereNomEnheter, tidligereNomEnheterReverseSorted_expected);

        assertEquals(tidligereNomEnheterReverseSorted, tidligereNomEnheterReverseSorted_expected);
    }

    private DvhRessursStilling getTestRessursStilling(String navid, String stillingAgressoId, String stillingOrgenhetNavn, LocalDate from, LocalDate to) {
        return getTestRessursStilling(navid, stillingAgressoId, stillingOrgenhetNavn, stillingAgressoId, from, to);
    }

    private DvhRessursStilling getTestRessursStilling(
        String navid,
        String stillingAgressoId,
        String stillingOrgenhetNavn,
        String ressursAgressoId,
        LocalDate from,
        LocalDate to
    ) {
        return DvhRessursStilling.builder()
            .navident(Optional.of(navid))
            .stillingAgressoId(Optional.of(stillingAgressoId))
            .stillingOrgenhetNavn(stillingOrgenhetNavn)
            .ressursAgressoId(Optional.of(ressursAgressoId))
            .ressursOrgenhetNiv(Optional.of("ORGENHET"))
            .stillingDatoFom(from)
            .stillingDatoTom(to)
            .build();
    }
}