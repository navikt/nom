package no.nav.nom.apps.api.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

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
}