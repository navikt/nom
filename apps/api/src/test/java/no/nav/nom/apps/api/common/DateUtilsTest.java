package no.nav.nom.apps.api.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertThrows;

class DateUtilsTest {

    @Test
    void isPeriodsOverlapping() {
        Assertions.assertTrue(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-12-31"), LocalDate.parse("2019-01-01"), LocalDate.parse("2019-12-31")));
        Assertions.assertTrue(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-12-31"), LocalDate.parse("2018-01-01"), LocalDate.parse("2019-08-31")));
        Assertions.assertTrue(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), null, LocalDate.parse("2018-01-01"), LocalDate.parse("2019-08-31")));
        Assertions.assertTrue(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-08-31"), LocalDate.parse("2018-01-01"), null));
        Assertions.assertTrue(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), null, LocalDate.parse("2018-01-01"), null));
        Assertions.assertTrue(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), null, LocalDate.parse("2019-01-01"), null));

        Assertions.assertFalse(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-12-31"), LocalDate.parse("2018-01-01"), LocalDate.parse("2018-12-31")));
        Assertions.assertFalse(DateUtils.isPeriodsOverlapping(LocalDate.parse("2018-01-01"), LocalDate.parse("2018-12-31"), LocalDate.parse("2019-01-01"), LocalDate.parse("2019-12-31")));
        Assertions.assertFalse(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), null, LocalDate.parse("2018-01-01"), LocalDate.parse("2018-12-31")));
    }

    @Test
    @DisplayName("Sluttdato cannot be before startdato")
    void test_sluttdato_cannot_be_before_startdato() {
        assertThrows(IllegalArgumentException.class, () -> DateUtils.validateChronologicalDates(LocalDate.now(), LocalDate.now().minusDays(1)));
    }

    @Test
    @DisplayName("Sluttdato can be null")
    void test_sluttdato_can_be_null() {
        DateUtils.validateChronologicalDates(LocalDate.now(), null);
    }

    @Test
    @DisplayName("Neste virkedag fungerer som tiltenkt")
    void test_nextWorkingDay() {
        if (LocalDate.now().getDayOfWeek() == DayOfWeek.FRIDAY) {
            Assertions.assertEquals(LocalDate.now().plusDays(3), DateUtils.nextWorkingDay());
        } else if (LocalDate.now().getDayOfWeek() == DayOfWeek.SATURDAY) {
            Assertions.assertEquals(LocalDate.now().plusDays(2), DateUtils.nextWorkingDay());
        } else {
            Assertions.assertEquals(LocalDate.now().plusDays(1), DateUtils.nextWorkingDay());
        }
    }

    @Test
    @DisplayName("Merge continuous periods")
    void test_mergeContinuousPeriods() {
        var periode1 = new DateUtils.Periode(LocalDate.now(), LocalDate.now().plusYears(1));
        var periode2 = new DateUtils.Periode(periode1.end().plusDays(1), LocalDate.now().plusYears(2));
        var periode3 = new DateUtils.Periode(periode1.end().plusDays(1), null);
        var periode4 = new DateUtils.Periode(null, LocalDate.now().plusMonths(18));
        var periode5 = new DateUtils.Periode(LocalDate.now(), null);
        var periode6 = new DateUtils.Periode(LocalDate.now().plusYears(4), null);

        var merged = DateUtils.mergeContinuousPeriods(List.of(periode1, periode2, periode3, periode4, periode5, periode6));
        Assertions.assertEquals(1, merged.size());
        Assertions.assertNull(merged.getFirst().start());
        Assertions.assertNull(merged.getFirst().end());

        merged = DateUtils.mergeContinuousPeriods(List.of(periode1, periode2));
        Assertions.assertEquals(1, merged.size());
        Assertions.assertEquals(periode1.start(), merged.getFirst().start());
        Assertions.assertEquals(periode2.end(), merged.getFirst().end());

        merged = DateUtils.mergeContinuousPeriods(List.of(periode1, periode2, periode6));
        Assertions.assertEquals(2, merged.size());
        Assertions.assertEquals(periode1.start(), merged.getFirst().start());
        Assertions.assertEquals(periode2.end(), merged.getFirst().end());
        Assertions.assertEquals(periode6.start(), merged.getLast().start());
        Assertions.assertEquals(periode6.end(), merged.getLast().end());

        merged = DateUtils.mergeContinuousPeriods(List.of(periode1, periode2, periode3, periode6));
        Assertions.assertEquals(1, merged.size());
        Assertions.assertEquals(periode1.start(), merged.getFirst().start());
        Assertions.assertNull(merged.getFirst().end());

        merged = DateUtils.mergeContinuousPeriods(List.of(periode1, periode2, periode4, periode6));
        Assertions.assertEquals(2, merged.size());
        Assertions.assertNull(merged.getFirst().start());
        Assertions.assertEquals(periode2.end(), merged.getFirst().end());
        Assertions.assertEquals(periode6.start(), merged.getLast().start());
        Assertions.assertEquals(periode6.end(), merged.getLast().end());
    }
}