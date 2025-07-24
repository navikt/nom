package no.nav.nom.libs.utils.common.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;

@UtilityClass
public class DateUtil {

    public static boolean isNow(LocalDate start, LocalDate end) {
        return (start == null || start.minusDays(1).isBefore(LocalDate.now())) &&
                (end == null || end.plusDays(1).isAfter(LocalDate.now()));
    }

    public static boolean sluttdatoIsAfterToday(LocalDate sluttdato) {
        return sluttdato != null ? sluttdato.isAfter(LocalDate.now().plusDays(1)) : LocalDate.MAX.isAfter(LocalDate.now().plusDays(1));
    }

    public static boolean startdatoIsAfterToday(LocalDate startdato) {
        return startdato != null ? startdato.isAfter(LocalDate.now()) : LocalDate.MIN.isAfter(LocalDate.now());
    }
}
