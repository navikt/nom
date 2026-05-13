package no.nav.nom.libs.utils.common.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.isNull;

public class DateUtils {

    public static String formatDateTime(LocalDate localDate) {
        if(localDate == null || localDate.toString().isEmpty() || localDate.toString().isBlank()){
            return "";
        }
        return DateTimeFormatter.ofPattern("dd.MM.yyyy").format(localDate);
    }

    public static boolean isPeriodActiveOrFuture(LocalDate periodStart, LocalDate periodEnd) {
        return isPeriodFuture(periodStart) || isPeriodActive(periodStart, periodEnd);
    }

    public static boolean isPeriodFuture(LocalDate periodStart) {
        periodStart = isNull(periodStart) ? LocalDate.now() : periodStart;
        var today = LocalDate.now();
        return periodStart.isAfter(today);
    }

    public static boolean isPeriodActive(LocalDate periodStart, LocalDate periodEnd) {
        periodStart = isNull(periodStart) ? LocalDate.now() : periodStart;
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

    public static boolean sluttdatoIsAfterToday(LocalDate sluttdato) {
        return sluttdato != null ? sluttdato.isAfter(LocalDate.now().plusDays(1)) : LocalDate.MAX.isAfter(LocalDate.now().plusDays(1));
    }

    public static boolean startdatoIsAfterToday(LocalDate startdato) {
        return startdato != null ? startdato.isAfter(LocalDate.now()) : LocalDate.MIN.isAfter(LocalDate.now());
    }

    public static boolean sluttdatoIsNotPassed(LocalDate sluttdato) {
        return sluttdato != null ? !sluttdato.isBefore(LocalDate.now()) : LocalDate.MAX.isAfter(LocalDate.now());
    }

    public static boolean overlapsInterval(LocalDate startDateA, LocalDate endDateA, LocalDate startDateB, LocalDate endDateB) {
        endDateA = endDateA == null ? LocalDate.MAX : endDateA;
        endDateB = endDateB == null ? LocalDate.MAX : endDateB;
        startDateA = startDateA == null ? LocalDate.MIN : startDateA;
        startDateB = startDateB == null ? LocalDate.MIN : startDateB;

        return overlapsInterval(
            new Periode(startDateA, endDateA),
            new Periode(startDateB, endDateB)
        );
    }

    public static boolean overlapsInterval(Periode period1, Periode period2) {
        return period1.start.isBefore(dayAfter(period2.end)) && period1.end.isAfter(dayBefore(period2.start));
    }

    public static boolean intervalCoversDate(LocalDate intervalStart, LocalDate intervalEnd, LocalDate date) {
        return likEllerFoer(intervalStart, date) && likEllerEtter(intervalEnd, date);
    }

    public static boolean likEllerFoer(LocalDate dato, LocalDate sjekkMotDato) {
        dato = dato == null ? LocalDate.MAX : dato;
        return dato.isBefore(dayAfter(sjekkMotDato == null ? LocalDate.MAX : sjekkMotDato));
    }

    public static boolean likEllerEtter(LocalDate dato, LocalDate sjekkMotDato) {
        dato = dato == null ? LocalDate.MAX : dato;
        return dato.isAfter(dayBefore(sjekkMotDato == null ? LocalDate.MAX : sjekkMotDato));
    }

    public static LocalDate dayAfter(LocalDate localDate) { return Objects.equals(LocalDate.MAX, localDate) ? LocalDate.MAX : localDate.plusDays(1); }

    public static LocalDate dayBefore(LocalDate localDate) { return localDate.minusDays(1); }

    public static boolean foer(LocalDate dato, LocalDate sjekkMotDato) {
        dato = dato == null ? LocalDate.MAX : dato;
        return dato.isBefore(sjekkMotDato == null ? LocalDate.MAX : sjekkMotDato);
    }

    public static boolean etter(LocalDate dato, LocalDate sjekkMotDato) {
        dato = dato == null ? LocalDate.MAX : dato;
        return dato.isAfter(sjekkMotDato == null ? LocalDate.MAX : sjekkMotDato);
    }

    public record Periode(
        LocalDate start,
        LocalDate end
    ) {}
}