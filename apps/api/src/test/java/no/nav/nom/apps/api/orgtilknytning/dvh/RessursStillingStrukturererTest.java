package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetService;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    private static IOrgenhet dummyEnhet_1;
    private static IOrgenhet dummyEnhet_2;

    @BeforeAll
    public static void init() {
        dummyEnhet_1 = getNomEnhet("FID_1", "x123yz", "Enhet_1", LocalDate.of(2000, 1, 1), null, "AGRID_1");
        dummyEnhet_2 = getNomEnhet("FID_2", "z321xy", "Enhet_2", LocalDate.of(2000, 1, 1), null, "AGRID_2");
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
        String navid_1 = "A001001";
        String navid_2 = "B002002";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE),
            getTestRessursStilling(navid_2, "AGRID_2", "Enhet_2", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_2, "AGRID_2", "Enhet_2", LocalDate.of(2020, 1, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_2, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        var result = ressursStillingStrukturerer.grupperPerNavident(ressursStillinger);
        assertEquals(2, result.size());
        assertEquals(3, result.get(navid_1).size());
        assertEquals(3, result.get(navid_2).size());
    }

    @Test
    @DisplayName("Test Gruppering per navident av RessursStillinger")
    public void test_002() {
        String navid_1 = "A001001";
        String navid_2 = "B002002";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_2, "AGRID_2", "Enhet_2", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_2, "AGRID_2", "Enhet_2", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_2, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        var result = ressursStillingStrukturerer.grupperPerNavident(ressursStillinger);
        assertEquals(2, result.size());
        assertEquals(3, result.get(navid_1).size());
        assertEquals(3, result.get(navid_2).size());
    }

    @Test
    @DisplayName("Test strukturer per navident av RessursStillinger")
    public void test_003() {
        String navid_1 = "A001001";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));
        var result = ressursStillingStrukturerer.strukturerNavidentensRessursStillinger(ressursStillinger);
        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2020, 1, 1), result.get(0).gyldigFom());
        assertEquals(MAX_DATE, result.get(0).gyldigTom());
    }

    @Test
    @DisplayName("Test strukturer per navident av RessursStillinger - Splitt på ikke sammenhengende periode")
    public void test_004() {
        String navid_1 = "A001001";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 8, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));
        var result = ressursStillingStrukturerer.strukturerNavidentensRessursStillinger(ressursStillinger);
        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2020, 1, 1), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2020, 8, 31), result.get(0).gyldigTom());
        assertEquals(LocalDate.of(2020, 11, 1), result.get(1).gyldigFom());
        assertEquals(MAX_DATE, result.get(1).gyldigTom());
    }

    @Test
    @DisplayName("Test strukturer per navident av RessursStillinger - Splitt på ulike enheter")
    public void test_005() {
        String navid_1 = "A001001";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_1, "AGRID_2", "Enhet_2", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));
        var result = ressursStillingStrukturerer.strukturerNavidentensRessursStillinger(ressursStillinger);
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
        String navid_1 = "A001001";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", LocalDate.of(2020, 3, 1), MAX_DATE),
            getTestRessursStilling(navid_1, "AGRID_2", "Enhet_2", LocalDate.of(2020, 11, 1), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "AGRID_2", "Enhet_2", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));
        var result = ressursStillingStrukturerer.strukturerNavidentensRessursStillinger(ressursStillinger);
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
        String navid_1 = "A001001";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_1, "AGRID_1", "SAMME NAVN", LocalDate.of(2020, 1, 1), MAX_DATE)
        );

        List<IOrgenhet> agrid_1_enheter = List.of(
            getNomEnhet("FID_1", "a123bc", "SAMME NAVN", LocalDate.of(2000, 1, 1), LocalDate.of(2020, 5,31), "AGRID_1"),
            getNomEnhet("FID_2", "x987yz", "SAMME NAVN", LocalDate.of(2000, 6, 1), null, "AGRID_1")
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(agrid_1_enheter);

        var result = ressursStillingStrukturerer.strukturerNavidentensRessursStillinger(ressursStillinger);
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
        String navid_1 = "A001001";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_1, "AGRID", "Enhet_1", "AGRID_1", LocalDate.of(2022, 1, 1), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1));
        var result = ressursStillingStrukturerer.strukturerNavidentensRessursStillinger(ressursStillinger);
        assertEquals(1, result.size());
        assertEquals("AGRID_1", result.get(0).stilling().ressursAgressoId().orElse("NULL"));
        assertEquals("FID_1", result.get(0).orgenhet().getFid());
    }

    private DvhRessursStilling getTestRessursStilling(String navid, String stillingAgressoId, String stillingOrgenhetNavn, LocalDate from, LocalDate to) {
        return getTestRessursStilling(navid, stillingAgressoId, stillingOrgenhetNavn, stillingAgressoId, from, to);
    }

    private DvhRessursStilling getTestRessursStilling(String navid, String stillingAgressoId, String stillingOrgenhetNavn, String ressursAgressoId, LocalDate from, LocalDate to) {
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