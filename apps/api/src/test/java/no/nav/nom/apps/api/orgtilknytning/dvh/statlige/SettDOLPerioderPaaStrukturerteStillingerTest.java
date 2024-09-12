package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@DisplayName("Tester for å sette DOL på strukturerte StillingPerioder fra DVH")
class SettDOLPerioderPaaStrukturerteStillingerTest extends RessursStillingStrukturererTestBase {
    @Autowired
    SettDOLPerioderPaaStrukturerteStillinger CUT; // Class under test

    @Test
    @DisplayName("Test finn brekk datoer - ingen OT'er")
    void test_finnBrekkDatoerIngenOT() {
        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2022, 11, 30)),
            stillingPeriode("A000001", "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
            stillingPeriode("A000001", "AGRID_3", "Enhet_3", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 12, 31))
        );

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);
        var result = CUT.finnBrekkDatoer(sspForNavident);

        List<LocalDate> expected = List.of(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2022, 1, 1),
            LocalDate.of(2022, 5, 1),
            LocalDate.of(2022, 12, 1),
            LocalDate.of(2023, 1, 1)
        );

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test finn brekk datoer - med OT'er")
    void test_finnBrekkDatoerMedOT() {
        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2022, 11, 30)),
            stillingPeriode("A000001", "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
            stillingPeriode("A000001", "AGRID_3", "Enhet_3", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 12, 31))
        );

        List<OrgtilknytningState> orgtilknytninger = List.of(
            orgtilknytning("A000001", "Enhet_1", LocalDate.of(2020, 4, 1), LocalDate.of(2022, 9, 30)),
            orgtilknytning("A000001", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
        );

        when(orgtilknytningStateRepository.findByNavident("A000001")).thenReturn(orgtilknytninger);

        StrukturerteStillingPerioderForNavident sspForNavident = new StrukturerteStillingPerioderForNavident("A000001", stillingPerioder, null);
        var result = CUT.finnBrekkDatoer(sspForNavident);

        List<LocalDate> expected = List.of(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 4, 1),
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2022, 1, 1),
            LocalDate.of(2022, 5, 1),
            LocalDate.of(2022, 10, 1),
            LocalDate.of(2022, 12, 1),
            LocalDate.of(2023, 1, 1)
        );

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test splitt StillingPerioder på brekk datoer")
    void test_splittStillingPerioderPaaBrekkDatoer() {
        List<StillingPeriode> stillingPerioder = List.of(
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2022, 11, 30)),
            stillingPeriode("A000001", "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
            stillingPeriode("A000001", "AGRID_3", "Enhet_3", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 12, 31)),
            stillingPeriode("A000001", "AGRID_4", "Enhet_4", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
        );

        List<LocalDate> brekkDatoer = List.of(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 4, 1),
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2022, 1, 1),
            LocalDate.of(2022, 5, 1),
            LocalDate.of(2022, 10, 1),
            LocalDate.of(2022, 12, 1),
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 2),
            LocalDate.of(2024, 12, 31),
            LocalDate.of(2025, 1, 1)
        );

        List<StillingPeriode> expected = List.of(
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 3, 31)),
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2020, 4, 1), LocalDate.of(2020, 12, 31)),
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 4, 30)),
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 9, 30)),
            stillingPeriode("A000001", "AGRID_1", "Enhet_1", LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 30)),
            stillingPeriode("A000001", "AGRID_2", "Enhet_2", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
            stillingPeriode("A000001", "AGRID_3", "Enhet_3", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 9, 30)),
            stillingPeriode("A000001", "AGRID_3", "Enhet_3", LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 30)),
            stillingPeriode("A000001", "AGRID_3", "Enhet_3", LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 31)),
            stillingPeriode("A000001", "AGRID_4", "Enhet_4", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
            stillingPeriode("A000001", "AGRID_4", "Enhet_4", LocalDate.of(2024, 1, 2), LocalDate.of(2024, 12, 30)),
            stillingPeriode("A000001", "AGRID_4", "Enhet_4", LocalDate.of(2024, 12, 31), LocalDate.of(2024, 12, 31))
        );

        var result = CUT.splittStillingPerioder(stillingPerioder, brekkDatoer);

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test finn historic fomDates - no current Fom dates")
    void test_finnHistoricFomDates_noCurrent() {
        LocalDate now = LocalDate.now();

        List<LocalDate> fomDates = List.of(
            now.minusYears(1),
            now.minusMonths(3),
            now.minusMonths(1)
        );

        var result = CUT.historicFomDates(fomDates);

        List<LocalDate> expected = List.of(
            now.minusYears(1),
            now.minusMonths(3)
        );

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test finn historic fomDates - no current Fom dates - end at today")
    void test_finnHistoricFomDates_noCurrent_endToday() {
        LocalDate now = LocalDate.now();

        List<LocalDate> fomDates = List.of(
            now.minusYears(1),
            now.minusMonths(3),
            now
        );

        var result = CUT.historicFomDates(fomDates);

        List<LocalDate> expected = List.of(
            now.minusYears(1),
            now.minusMonths(3)
        );

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test finn historic fomDates - with current and future dates")
    void test_finnHistoricFomDates_currentAndFutureDates() {
        LocalDate now = LocalDate.now();

        List<LocalDate> fomDates = List.of(
            now.minusYears(1),
            now.minusMonths(3),
            now.minusMonths(1),
            now.plusMonths(1),
            now.plusYears(1)
        );

        var result = CUT.historicFomDates(fomDates);

        List<LocalDate> expected = List.of(
            now.minusYears(1),
            now.minusMonths(3)
        );

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test finn historic fomDates - only future dates")
    void test_finnHistoricFomDates_onlyFutureDates() {
        LocalDate now = LocalDate.now();

        List<LocalDate> fomDates = List.of(
            now,
            now.plusMonths(1),
            now.plusYears(1)
        );

        var result = CUT.historicFomDates(fomDates);

        List<LocalDate> expected = List.of();

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test finn current fomDates")
    void test_finnCurrentFomDates() {
        LocalDate now = LocalDate.now();

        List<LocalDate> fomDates = List.of(
            now.minusYears(1),
            now.minusMonths(3),
            now.minusMonths(1)
        );

        var result = CUT.currentFomDate(fomDates);
        assertEquals(now.minusMonths(1) , result);

        fomDates = List.of(
            now.minusYears(1),
            now.minusMonths(3),
            now
        );

        result = CUT.currentFomDate(fomDates);
        assertEquals(now, result);

        result = CUT.currentFomDate(List.of());
        assertNull(result);

        fomDates = List.of(
            now.plusMonths(3),
            now.plusYears(1)
        );

        result = CUT.currentFomDate(fomDates);
        assertNull(result);
    }

    @Test
    @DisplayName("Test finn future fomDates")
    void test_finnFutureFomDates() {
        LocalDate now = LocalDate.now();

        List<LocalDate> fomDates = List.of(
            now.minusYears(1),
            now.minusMonths(3),
            now
        );

        var result = CUT.futureFomDates(fomDates);
        assertEquals(List.of() , result);

        fomDates = List.of(
            now.minusYears(1),
            now.minusMonths(3),
            now.minusMonths(1),
            now.plusMonths(1),
            now.plusYears(1)
        );

        List<LocalDate> expected = List.of(
            now.plusMonths(1),
            now.plusYears(1)
        );

        result = CUT.futureFomDates(fomDates);
        assertEquals(expected, result);
    }
}