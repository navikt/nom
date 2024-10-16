package no.nav.nom.apps.api.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DateUtils {

    public static String formatDateTime(LocalDate localDate) {
        if(localDate == null || localDate.toString().isEmpty() || localDate.toString().isBlank()){
            return "";
        }
        return DateTimeFormatter.ofPattern("dd.MM.YYYY").format(localDate);
    }

    public static boolean isPeriodsOverlapping(LocalDate period1Start, Optional<LocalDate> period1End, LocalDate period2Start, Optional<LocalDate> period2End) {
        return isPerioderOverlapping(
            new Periode(period1Start, period1End.orElse(LocalDate.MAX)),
            new Periode(period2Start, period2End.orElse(LocalDate.MAX))
        );
    }

    public static Boolean isPerioderOverlapping(Periode periode1, Periode periode2) {
        return periode1.start().isBefore(periode2.end) && periode2.start.isBefore(periode1.end);
    }

    public static boolean isPeriodActive(LocalDate periodStart, Optional<LocalDate> periodEnd) {
        return isPeriodActive(periodStart, periodEnd.orElse(null));
    }

    public static boolean isPeriodActive(LocalDate periodStart, LocalDate periodEnd) {
        var today = LocalDate.now();
        var isPeriodStartBeforeOrEqualToToday = periodStart.isBefore(today.plusDays(1));
        var isPeriodEndAfterOrEqualToToday = periodEnd == null || periodEnd.isAfter(today.minusDays(1));

        return isPeriodStartBeforeOrEqualToToday && isPeriodEndAfterOrEqualToToday;
    }

    public static void validateChronologicalDates(LocalDate startDato, LocalDate sluttDato) {
        if (sluttDato != null) {
            if (sluttDato.isBefore(startDato)) {
                throw new IllegalArgumentException("Sluttdato kan ikke være før startdato");
            }
        }
    }

    public static LocalDate nextWorkingDay() {
        LocalDate today = LocalDate.now();

        if(today.getDayOfWeek() == DayOfWeek.FRIDAY) {
            return today.plusDays(3);
        }

        if(today.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return today.plusDays(2);
        }

        return today.plusDays(1);
    }

    public record Periode(
        LocalDate start,
        LocalDate end
    ) {}
}