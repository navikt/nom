package no.nav.nom.apps.api.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.Assert.assertThrows;

class DateUtilsTest {

    @Test
    void isPeriodsOverlapping() {
        Assertions.assertTrue(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), Optional.of(LocalDate.parse("2019-12-31")), LocalDate.parse("2019-01-01"), Optional.of(LocalDate.parse("2019-12-31"))));
        Assertions.assertTrue(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), Optional.of(LocalDate.parse("2019-12-31")), LocalDate.parse("2018-01-01"), Optional.of(LocalDate.parse("2019-08-31"))));
        Assertions.assertTrue(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), Optional.empty(), LocalDate.parse("2018-01-01"), Optional.of(LocalDate.parse("2019-08-31"))));
        Assertions.assertTrue(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), Optional.of(LocalDate.parse("2019-08-31")), LocalDate.parse("2018-01-01"), Optional.empty()));
        Assertions.assertTrue(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), Optional.empty(), LocalDate.parse("2018-01-01"), Optional.empty()));
        Assertions.assertTrue(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), Optional.empty(), LocalDate.parse("2019-01-01"), Optional.empty()));

        Assertions.assertFalse(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), Optional.of(LocalDate.parse("2019-12-31")), LocalDate.parse("2018-01-01"), Optional.of(LocalDate.parse("2018-12-31"))));
        Assertions.assertFalse(DateUtils.isPeriodsOverlapping(LocalDate.parse("2018-01-01"), Optional.of(LocalDate.parse("2018-12-31")), LocalDate.parse("2019-01-01"), Optional.of(LocalDate.parse("2019-12-31"))));
        Assertions.assertFalse(DateUtils.isPeriodsOverlapping(LocalDate.parse("2019-01-01"), Optional.empty(), LocalDate.parse("2018-01-01"), Optional.of(LocalDate.parse("2018-12-31"))));
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
}