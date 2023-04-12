package no.nav.nom.apps.api.orgTilknytning.dvh;

import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tester gruppering og periodisering av RessursStillinger fra DVH")
class RessursStillingLoaderTest extends TestBase {
    private static final LocalDate MAX_DATE = LocalDate.of(2099, 12, 31);

    @Autowired
    private RessursStillingLoader ressursStillingLoader;

    @Test
    @DisplayName("Test Gruppering per navident av RessursStillinger")
    public void testGrupperRessursStillinger() {
        String navid_1 = "A001001";
        String navid_2 = "B002002";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_1, "1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_1, "1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "1", "Enhet_1", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE),
            getTestRessursStilling(navid_2, "2", "Enhet_2", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_2, "2", "Enhet_2", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_2, "2", "Enhet_2", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE)
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
            getTestRessursStilling(navid_2, "2", "Enhet_2", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_2, "2", "Enhet_2", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_1, "1", "Enhet_1", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE),
            getTestRessursStilling(navid_1, "1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_2, "2", "Enhet_2", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE)
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
            getTestRessursStilling(navid_1, "1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_1, "1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "1", "Enhet_1", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE)
        );

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
            getTestRessursStilling(navid_1, "1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 8, 31)),
            getTestRessursStilling(navid_1, "1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "1", "Enhet_1", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE)
        );

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
            getTestRessursStilling(navid_1, "1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_1, "2", "Enhet_2", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "2", "Enhet_2", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE)
        );

        var result = ressursStillingLoader.strukturerNavidentensRessursStillinger(ressursStillinger);
        assertEquals(2, result.size());
        assertEquals("1", result.get(0).stilling().ressursAgressoId().get());
        assertEquals(LocalDate.of(2020, 01, 01), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2020, 10, 31), result.get(0).gyldigTom());
        assertEquals("2", result.get(1).stilling().ressursAgressoId().get());
        assertEquals(LocalDate.of(2020, 11, 01), result.get(1).gyldigFom());
        assertEquals(MAX_DATE, result.get(1).gyldigTom());
    }

    @Test
    @DisplayName("Test strukturer per navident av RessursStillinger - Splitt på ulike enheter - Skip overlapp")
    public void testStrukturerNavidentensRessursStillinger_SkippOverlapp() {
        String navid_1 = "A001001";
        List<DvhRessursStilling> ressursStillinger = List.of(
            getTestRessursStilling(navid_1, "1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 01, 01), LocalDate.of(2020, 10, 31)),
            getTestRessursStilling(navid_1, "1", "Enhet_1", "ORGENHET", LocalDate.of(2020, 03, 01), MAX_DATE),
            getTestRessursStilling(navid_1, "2", "Enhet_2", "ORGENHET", LocalDate.of(2020, 11, 01), LocalDate.of(2021, 12, 31)),
            getTestRessursStilling(navid_1, "2", "Enhet_2", "ORGENHET", LocalDate.of(2022, 01, 01), MAX_DATE)
        );

        var result = ressursStillingLoader.strukturerNavidentensRessursStillinger(ressursStillinger);
        assertEquals(2, result.size());
        assertEquals("1", result.get(0).stilling().ressursAgressoId().get());
        assertEquals(LocalDate.of(2020, 01, 01), result.get(0).gyldigFom());
        assertEquals(LocalDate.of(2020, 10, 31), result.get(0).gyldigTom());
        assertEquals("2", result.get(1).stilling().ressursAgressoId().get());
        assertEquals(LocalDate.of(2020, 11, 01), result.get(1).gyldigFom());
        assertEquals(MAX_DATE, result.get(1).gyldigTom());
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
}