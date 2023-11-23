package no.nav.nom.apps.api.common;

import java.time.LocalDate;
import java.util.Optional;

public class DateUtils {
    public static boolean isPeriodsOverlapping(LocalDate period1Start, Optional<LocalDate> period1End, LocalDate period2Start, Optional<LocalDate> period2End) {
        return period1Start.isBefore(period2End.orElse(LocalDate.MAX)) && period2Start
            .isBefore(period1End.orElse(LocalDate.MAX));
    }

    public static void validateChronologicalDates(LocalDate startDato, LocalDate sluttDato) {
        if (sluttDato != null) {
            if (sluttDato.isBefore(startDato)) {
                throw new IllegalArgumentException("Sluttdato kan ikke være før startdato");
            }
        }
    }
}