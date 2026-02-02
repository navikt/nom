package no.nav.nom.libs.utils.common.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.Objects;

@UtilityClass
public class DateUtil {
    public static boolean isActive(LocalDate start, LocalDate end) {
        return (start == null || start.minusDays(1).isBefore(LocalDate.now())) &&
                (end == null || end.plusDays(1).isAfter(LocalDate.now()));
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

        return startDateA.isBefore(dayAfter(endDateB)) && endDateA.isAfter(dayBefore(startDateB));
    }

    public static boolean foer(LocalDate dato, LocalDate sjekkMotDato) {
        dato = dato == null ? LocalDate.MAX : dato;
        return dato.isBefore(sjekkMotDato == null ? LocalDate.MAX : sjekkMotDato);
    }

    public boolean etter(LocalDate dato, LocalDate sjekkMotDato) {
        dato = dato == null ? LocalDate.MAX : dato;
        return dato.isAfter(sjekkMotDato == null ? LocalDate.MAX : sjekkMotDato);
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
}
