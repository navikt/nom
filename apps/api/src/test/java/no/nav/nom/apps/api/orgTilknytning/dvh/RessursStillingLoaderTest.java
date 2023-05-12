package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetService;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader.UNLEASH_LAST_NY_ORGTILKNYTNING;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@DisplayName("Tester gruppering og periodisering av RessursStillinger fra DVH")
class RessursStillingLoaderTest extends TestBase {
    private static final LocalDate MAX_DATE = LocalDate.of(2099, 12, 31);

    @Autowired
    private RessursStillingLoader ressursStillingLoader;

    @MockBean
    OrgenhetService orgenhetService;

    private static IOrgenhet dummyEnhet_1;
    private static IOrgenhet dummyEnhet_2;

    @BeforeAll
    public static void init() {
        dummyEnhet_1 = new OrgEnhet(new OrgEnhetState("FID_1", "x123yz", "Enhet_1", LocalDate.of(2000, 1, 1), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("AGRID_1-ORGENHET-Enhet_1"), Optional.of("AGRID_1"), Optional.of("ORGENHET"), Optional.empty() ) );
        dummyEnhet_2 = new OrgEnhet(new OrgEnhetState("FID_2", "z321xy", "Enhet_2", LocalDate.of(2000, 1, 1), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("AGRID_2-ORGENHET-Enhet_2"), Optional.of("AGRID_2"), Optional.of("ORGENHET"), Optional.empty() ) );
    }

    @Test
    @DisplayName("Test Gruppering per navident av RessursStillinger")
    public void testGrupperRessursStillinger() {
        String navid_1 = "A001001";
        String navid_2 = "B002002";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE),
            getTestRessursStilling(navid_2, "AGRID_2", "Enhet_2", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_2, "AGRID_2", "Enhet_2", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_2, "AGRID_2", "Enhet_2", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE)
        );

        var result = ressursStillingLoader.grupperPerNavident(ressursStillinger);
        assertEquals(2, result.size());
        assertEquals(3, result.get(navid_1).size());
        assertEquals(3, result.get(navid_2).size());
    }

    @Test
    @DisplayName("Test Gruppering per navident av RessursStillinger")
    public void testGrupperRessursStillingerUsortert() {
        String navid_1 = "A001001";
        String navid_2 = "B002002";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_2, "AGRID_2", "Enhet_2", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_2, "AGRID_2", "Enhet_2", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_2, "AGRID_2", "Enhet_2", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE)
        );

        var result = ressursStillingLoader.grupperPerNavident(ressursStillinger);
        assertEquals(2, result.size());
        assertEquals(3, result.get(navid_1).size());
        assertEquals(3, result.get(navid_2).size());
    }

    @Test
    @DisplayName("Test strukturer per navident av RessursStillinger")
    public void testStrukturerNavidentensRessursStillinger() {
        String navid_1 = "A001001";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));
        var result = ressursStillingLoader.strukturerNavidentensRessursStillinger(ressursStillinger);
        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2020, 01, 01), result.get(0).gyldigFom());
        assertEquals(MAX_DATE, result.get(0).gyldigTom());
    }

    @Test
    @DisplayName("Test strukturer per navident av RessursStillinger - Splitt på ikke sammenhengende periode")
    public void testStrukturerNavidentensRessursStillinger_SplittPåPeriode() {
        String navid_1 = "A001001";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 8, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));
        var result = ressursStillingLoader.strukturerNavidentensRessursStillinger(ressursStillinger);
        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2020, 01, 01), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2020, 8, 31), result.get(0).gyldigTom());
        assertEquals(LocalDate.of(2020, 11, 01), result.get(1).gyldigFom());
        assertEquals(MAX_DATE, result.get(1).gyldigTom());
    }

    @Test
    @DisplayName("Test strukturer per navident av RessursStillinger - Splitt på ulike enheter")
    public void testStrukturerNavidentensRessursStillinger_SplittPåUlikeEnheter() {
        String navid_1 = "A001001";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_1, "AGRID_2", "Enhet_2", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "AGRID_2", "Enhet_2", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));
        var result = ressursStillingLoader.strukturerNavidentensRessursStillinger(ressursStillinger);
        assertEquals(2, result.size());
        assertEquals("AGRID_1", result.get(0).stilling().ressursAgressoId().get());
        assertEquals(LocalDate.of(2020, 01, 01), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2020, 10, 31), result.get(0).gyldigTom());
        assertEquals("AGRID_2", result.get(1).stilling().ressursAgressoId().get());
        assertEquals(LocalDate.of(2020, 11, 01), result.get(1).gyldigFom());
        assertEquals(MAX_DATE, result.get(1).gyldigTom());
    }

    @Test
    @DisplayName("Test strukturer per navident av RessursStillinger - Splitt på ulike enheter - Skip overlapp")
    public void testStrukturerNavidentensRessursStillinger_SkippOverlapp() {
        String navid_1 = "A001001";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_1, "AGRID_1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 03, 01), MAX_DATE),
            getTestRessursStilling(navid_1, "AGRID_2", "Enhet_2", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "AGRID_2", "Enhet_2", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE)
        );

        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));
        var result = ressursStillingLoader.strukturerNavidentensRessursStillinger(ressursStillinger);
        assertEquals(2, result.size());
        assertEquals("AGRID_1", result.get(0).stilling().ressursAgressoId().get());
        assertEquals(LocalDate.of(2020, 01, 01), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2020, 10, 31), result.get(0).gyldigTom());
        assertEquals("AGRID_2", result.get(1).stilling().ressursAgressoId().get());
        assertEquals(LocalDate.of(2020, 11, 01), result.get(1).gyldigFom());
        assertEquals(MAX_DATE, result.get(1).gyldigTom());
    }

    @Test
    @DisplayName("Test last av Resssurs stillinger fra Dvh json")
    public void testLoad() {
        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));

        /* Se last innhold nederst i denne filen */
        var result = ressursStillingLoader.load();

        assertEquals(21, result.size());
        assertEquals(12, result.stream().filter( rs -> rs.getNavident().equals(Optional.of("G123321"))).count());
        assertEquals(LocalDate.of(2020, 1, 01), result.stream().filter( rs -> rs.getNavident().equals(Optional.of("G123321"))).sorted(Comparator.comparing(DvhRessursStilling::getStillingDatoFom)).findFirst().get().getStillingDatoFom());
        assertEquals(LocalDate.of(2021, 10, 01), result.stream().filter( rs -> rs.getNavident().equals(Optional.of("G123321"))).sorted(Comparator.comparing(DvhRessursStilling::getStillingDatoFom).reversed()).findFirst().get().getStillingDatoFom());
        assertEquals(9, result.stream().filter( rs -> rs.getNavident().equals(Optional.of("H554433"))).count());
        assertEquals(LocalDate.of(2020, 8, 15), result.stream().filter( rs -> rs.getNavident().equals(Optional.of("H554433"))).sorted(Comparator.comparing(DvhRessursStilling::getStillingDatoFom)).findFirst().get().getStillingDatoFom());
        assertEquals(LocalDate.of(2021, 9, 01), result.stream().filter( rs -> rs.getNavident().equals(Optional.of("H554433"))).sorted(Comparator.comparing(DvhRessursStilling::getStillingDatoFom).reversed()).findFirst().get().getStillingDatoFom());
    }

    @Test
    @DisplayName("Test last og prossesering av Resssurs stillinger fra Dvh json")
    public void testloadAndProcess() {
        /* Se last innhold nederst i denne filen */
        Mockito.when(unleash.isEnabled(UNLEASH_LAST_NY_ORGTILKNYTNING)).thenReturn(true);
        when(orgenhetService.matchByAgressoId(any())).thenReturn(List.of(dummyEnhet_1, dummyEnhet_2));

        ressursStillingLoader.loadAndProcess();
    }

    private DvhRessursStilling getTestRessursStilling(String navid, String stillingAgressoId, String stillingOrgenhetNavn, String ressursOrgenhetNiv, LocalDate from, LocalDate to) {
        return getTestRessursStilling(navid, stillingAgressoId, stillingOrgenhetNavn, stillingAgressoId, ressursOrgenhetNiv, from, to);
    }

    private DvhRessursStilling getTestRessursStilling(String navid, String stillingAgressoId, String stillingOrgenhetNavn, String ressursAgressoId, String ressursOrgenhetNiv, LocalDate from, LocalDate to) {
        return DvhRessursStilling.builder()
            .navident(Optional.of(navid))
            .stillingAgressoId(Optional.of(stillingAgressoId))
            .stillingOrgenhetNavn(stillingOrgenhetNavn)
            .ressursAgressoId(Optional.of(ressursAgressoId))
            .ressursOrgenhetNiv(Optional.of(ressursOrgenhetNiv))
            .stillingDatoFom(from)
            .stillingDatoTom(to)
            .build();
    }

/* Content of dvhRessursStillinger.json
    NAV_ID  RESOURCE_ID               FIRST_NAME                                         SURNAME                                            FRA_DATO   TIL_DATO   POST_DATE_ ANSETTELSESTYPE_BESK                                                                                                                                                                                                                                            PARTTIME_PCT ORGENHET                  ORGENHET_BESK                                                                                                                                                                                                                                                   LEDER_PAA_STILLING        PERM_KODE                 LAST_UPDAT ORGENHET_PAA_RESSURS                                                                                 ORGENHET_PAA_RESSURS_NIVA                                                                            REME STILLING_ORGENHET_PAA_RES AGRESSO_DA AGRESSO_DA DATE_FROM  DATE_TO    LEDER_RESOURCE_ID         SYKEMLEDER_RESOURCE_ID   
    ------- ------------------------- -------------------------------------------------- -------------------------------------------------- ---------- ---------- ---------- --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- ------------ ------------------------- --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- ------------------------- ------------------------- ---------- ---------------------------------------------------------------------------------------------------- ---------------------------------------------------------------------------------------------------- ---- ------------------------- ---------- ---------- ---------- ---------- ------------------------- -------------------------
    G123321 123321                    Tom                                                Bombadil                                           01.01.2020 31.05.2020 31.05.2020 Fast                                                                                                                                                                                                                                                                      60 8620005                   Data og innsikt gruppe 5                                                                                                                                                                                                                                        153469                    ARB(EGEN)                 14.05.2020 8552402                                                                                              ORGENHET                                                                                             2920 8620005                   01.02.2019 31.12.2099 31.10.2018 31.12.2099 166503                    166503
    G123321 123321                    Tom                                                Bombadil                                           01.01.2020 31.05.2020 31.05.2020 Innlån/utlån                                                                                                                                                                                                                                                              40 8620005                   Data og innsikt gruppe 5                                                                                                                                                                                                                                        153469                                              14.05.2020 8552402                                                                                              ORGENHET                                                                                             2920 8620005                   01.02.2019 31.12.2099 31.10.2018 31.12.2099 166503                    166503
    G123321 123321                    Tom                                                Bombadil                                           01.06.2020 03.06.2020 30.06.2020 Innlån/utlån                                                                                                                                                                                                                                                              40 8552106                   IT utvikling 6                                                                                                                                                                                                                                                  153469                                              14.05.2020 8552402                                                                                              ORGENHET                                                                                             2920 8552106                   01.02.2019 31.12.2099 31.10.2018 31.12.2099 166503                    166503
    G123321 123321                    Tom                                                Bombadil                                           01.06.2020 03.06.2020 30.06.2020 Fast                                                                                                                                                                                                                                                                      60 8552106                   IT utvikling 6                                                                                                                                                                                                                                                  153469                    ARB(EGEN)                 14.05.2020 8552402                                                                                              ORGENHET                                                                                             2920 8552106                   01.02.2019 31.12.2099 31.10.2018 31.12.2099 166503                    166503
    G123321 123321                    Tom                                                Bombadil                                           04.06.2020 30.06.2020 30.06.2020 Innlån/utlån                                                                                                                                                                                                                                                              40 8552106                   IT utvikling 6                                                                                                                                                                                                                                                  153469                                              14.05.2020 8552402                                                                                              ORGENHET                                                                                             2920 8552106                   01.02.2019 31.12.2099 31.10.2018 31.12.2099 166503                    166503
    G123321 123321                    Tom                                                Bombadil                                           04.06.2020 30.06.2020 30.06.2020 Fast                                                                                                                                                                                                                                                                      60 8552106                   IT utvikling 6                                                                                                                                                                                                                                                  153469                    ARB(EGEN)                 14.05.2020 8552402                                                                                              ORGENHET                                                                                             2920 8552106                   01.02.2019 31.12.2099 31.10.2018 31.12.2099 166503                    166503
    G123321 123321                    Tom                                                Bombadil                                           01.07.2020 30.09.2020 31.01.2021 Fast                                                                                                                                                                                                                                                                     100 8552106                   IT utvikling 6                                                                                                                                                                                                                                                  153469                                              21.01.2021 8552402                                                                                              ORGENHET                                                                                             2920 8552106                   01.02.2019 31.12.2099 31.10.2018 31.12.2099 166503                    166503
    G123321 123321                    Tom                                                Bombadil                                           01.10.2020 31.01.2021 31.01.2021 Fast                                                                                                                                                                                                                                                                     100 8552106                   IT utvikling 6                                                                                                                                                                                                                                                  153469                                              21.01.2021 8552402                                                                                              ORGENHET                                                                                             2920 8552106                   01.02.2019 31.12.2099 31.10.2018 31.12.2099 166503                    166503
    G123321 123321                    Tom                                                Bombadil                                           01.02.2021 30.04.2021 31.07.2021 Fast                                                                                                                                                                                                                                                                      40 8552106                   IT utvikling 6                                                                                                                                                                                                                                                  153469                    AAP                       25.10.2021 8552402                                                                                              ORGENHET                                                                                             2920 8552106                   01.02.2019 31.12.2099 31.10.2018 31.12.2099 166503                    166503
    G123321 123321                    Tom                                                Bombadil                                           01.05.2021 31.07.2021 31.07.2021 Fast                                                                                                                                                                                                                                                                      40 8552106                   IT utvikling 6                                                                                                                                                                                                                                                  153469                    AAP                       25.10.2021 8552402                                                                                              ORGENHET                                                                                             2920 8552106                   01.02.2019 31.12.2099 31.10.2018 31.12.2099 166503                    166503
    G123321 123321                    Tom                                                Bombadil                                           01.08.2021 30.09.2021 30.09.2021 Fast                                                                                                                                                                                                                                                                       0 8552106                   IT utvikling 6                                                                                                                                                                                                                                                  153469                    AAP                       25.10.2021 8552402                                                                                              ORGENHET                                                                                             2920 8552106                   01.02.2019 31.12.2099 31.10.2018 31.12.2099 166503                    166503
    G123321 123321                    Tom                                                Bombadil                                           01.10.2021 31.01.2022 31.03.2022 Fast                                                                                                                                                                                                                                                                      40 8552106                   IT utvikling 6                                                                                                                                                                                                                                                  153469                    AAP                       10.03.2022 8552402                                                                                              ORGENHET                                                                                             2920 8552106                   01.02.2019 31.12.2099 31.10.2018 31.12.2099 166503                    166503

    H554433 554433                    Ola                                                Normann                                            15.08.2020 31.08.2020 31.12.2020 Midlertidig karakter § 9a                                                                                                                                                                                                                                                 60 0807001                   Notodden lag 1, 2 og 3                                                                                                                                                                                                                                          165646                                              23.12.2020 080700                                                                                               ORGNIV3                                                                                              0807 0807001                   15.08.2020 30.06.2021 03.07.2020 31.12.2099 122805                    122805
    H554433 554433                    Ola                                                Normann                                            01.09.2020 30.09.2020 31.12.2020 Midlertidig karakter § 9a                                                                                                                                                                                                                                                 60 0807001                   Notodden lag 1, 2 og 3                                                                                                                                                                                                                                          165646                                              23.12.2020 080700                                                                                               ORGNIV3                                                                                              0807 0807001                   15.08.2020 30.06.2021 03.07.2020 31.12.2099 122805                    122805
    H554433 554433                    Ola                                                Normann                                            01.10.2020 30.11.2020 31.12.2020 Midlertidig karakter § 9a                                                                                                                                                                                                                                                 60 0807001                   Notodden lag 1, 2 og 3                                                                                                                                                                                                                                          165646                                              23.12.2020 080700                                                                                               ORGNIV3                                                                                              0807 0807001                   15.08.2020 30.06.2021 03.07.2020 31.12.2099 122805                    122805
    H554433 554433                    Ola                                                Normann                                            01.12.2020 31.12.2020 31.12.2020 Midlertidig karakter § 9a                                                                                                                                                                                                                                                 60 0807001                   Notodden lag 1, 2 og 3                                                                                                                                                                                                                                          165646                                              23.12.2020 080700                                                                                               ORGNIV3                                                                                              0807 0807001                   15.08.2020 30.06.2021 03.07.2020 31.12.2099 122805                    122805
    H554433 554433                    Ola                                                Normann                                            01.01.2021 30.04.2021 30.06.2021 Midlertidig karakter § 9a                                                                                                                                                                                                                                                 60 0807001                   Notodden lag 1, 2 og 3                                                                                                                                                                                                                                          165646                                              31.01.2021 080700                                                                                               ORGNIV3                                                                                              0807 0807001                   15.08.2020 30.06.2021 03.07.2020 31.12.2099 122805                    122805
    H554433 554433                    Ola                                                Normann                                            01.05.2021 30.06.2021 30.06.2021 Midlertidig karakter § 9a                                                                                                                                                                                                                                                 60 0807001                   Notodden lag 1, 2 og 3                                                                                                                                                                                                                                          165646                                              31.01.2021 080700                                                                                               ORGNIV3                                                                                              0807 0807001                   15.08.2020 30.06.2021 03.07.2020 31.12.2099 122805                    122805
    H554433 554433                    Ola                                                Normann                                            01.07.2021 31.07.2021 31.12.2099 Kommunalt ansatt                                                                                                                                                                                                                                                           0 -4                        Mangler                                                                                                                                                                                                                                                                                                             08.07.2021 080700                                                                                               ORGNIV3                                                                                              0807 0807001                   15.08.2020 30.06.2021 03.07.2020 31.12.2099 122805                    122805
    H554433 554433                    Ola                                                Normann                                            01.08.2021 31.08.2021 31.12.2099 Kommunalt ansatt                                                                                                                                                                                                                                                           0 -4                        Mangler                                                                                                                                                                                                                                                                                                             08.07.2021 080700                                                                                               ORGNIV3                                                                                              0807 0807001                   15.08.2020 30.06.2021 03.07.2020 31.12.2099 122805                    122805
    H554433 554433                    Ola                                                Normann                                            01.09.2021 31.12.2099 31.12.2099 Kommunalt ansatt                                                                                                                                                                                                                                                           0 -4                        Mangler                                                                                                                                                                                                                                                                                                             08.07.2021 080700                                                                                               ORGNIV3                                                                                              0807 0807001                   15.08.2020 30.06.2021 03.07.2020 31.12.2099 122805                    122805
*/


}