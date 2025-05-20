package no.nav.nom.apps.api.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class DateUtils {

    public static String formatDateTime(LocalDate localDate) {
        if(localDate == null || localDate.toString().isEmpty() || localDate.toString().isBlank()){
            return "";
        }
        return DateTimeFormatter.ofPattern("dd.MM.yyyy").format(localDate);
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

    public static boolean isPeriodActive(Optional<LocalDate> periodStart, Optional<LocalDate> periodEnd) {
        if (periodStart.isEmpty()) {
            return false;
        }
        return isPeriodActive(periodStart.orElse(null), periodEnd.orElse(null));
    }

    public static boolean isPeriodActive(LocalDate periodStart, LocalDate periodEnd) {
        var today = LocalDate.now();
        var isPeriodStartBeforeOrEqualToToday = periodStart.isBefore(today.plusDays(1));
        var isPeriodEndAfterOrEqualToToday = periodEnd == null || periodEnd.isAfter(today.minusDays(1));

        return isPeriodStartBeforeOrEqualToToday && isPeriodEndAfterOrEqualToToday;
    }

    public static boolean isPeriodFuture(LocalDate periodStart, Optional<LocalDate> periodEnd) {
        var today = LocalDate.now();
        return periodStart.isAfter(today) && (periodEnd.isEmpty() || periodEnd.get().isAfter(today));
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

    public static boolean isPeriodCoveredBy(LocalDate periodFom, LocalDate periodTom, List<Periode> perioder) {
        return perioder.stream().anyMatch(p -> isPeriodCoveredByAny(periodFom, periodTom, mergeContinuousPeriods(perioder)));
    }

    public static List<Periode> mergeContinuousPeriods(List<Periode> perioder) {
        var sorted = perioder.stream()
            .sorted(Comparator.comparing(p -> orMin(p.start())))
            .toList();

        List<Periode> merged = new ArrayList<>();
        Periode current = null;
        for (Periode p : sorted) {
            var start = orMin(p.start());
            var end = orMax(p.end());
            if (current == null) {
                current = new Periode(orNull(start), orNull(end));
            } else {
                if (orMax(current.end()).isBefore(start.minusDays(1))) {
                    merged.add(current);
                    current = new Periode(orNull(start), orNull(end));
                } else {
                    current = new Periode(current.start(), orNull(end));
                }
            }
        }
        merged.add(current);
        return merged;
    }

    public static boolean isPeriodCoveredByAny(LocalDate periodFom, LocalDate periodTom, List<Periode> periode) {
        return periode.stream().anyMatch(p -> isPeriodCoveredBy(periodFom, periodTom, p));
    }

    public static boolean isPeriodCoveredBy(LocalDate periodFom, LocalDate periodTom, Periode periode) {
        return isPeriodCoveredBy(periodFom, periodTom, periode.start(), periode.end());
    }

    public static boolean isPeriodCoveredBy(LocalDate periodFom, LocalDate periodTom, LocalDate fom, LocalDate tom) {
        periodFom = orMin(periodFom);
        periodTom = orMax(periodTom);
        fom = orMin(fom);
        tom = orMax(tom);

        return !fom.isAfter(periodFom) && !tom.isBefore(periodTom);
    }

    private static LocalDate orMin(LocalDate date) {
        return date == null ? LocalDate.MIN : date;
    }
    private static LocalDate orMax(LocalDate date) {
        return date == null ? LocalDate.MAX : date;
    }

    private static LocalDate orNull(LocalDate date) {
        return LocalDate.MIN.equals(date) || LocalDate.MAX.equals(date) ? null : date;
    }

    public record Periode(
        LocalDate start,
        LocalDate end
    ) {}
}