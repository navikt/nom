package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.contract.nom.state.OrgtilknytningState;

import java.time.LocalDate;

public class PeriodeUtil {
    public static final LocalDate NO_END_DATE = LocalDate.of(2099, 12, 31);

    public static boolean isFuturePeriod(OrgtilknytningState orgtilknytning) {
        return orgtilknytning.gyldigFom().isAfter(LocalDate.now());
    }
    public static boolean isFuturePeriod(StillingPeriode stillingPeriode) {
        return stillingPeriode.gyldigFom().isAfter(LocalDate.now());
    }

    public static boolean isCurrentPeriod(StillingPeriode stillingPeriode) {
        return isCurrentOrFuturePeriod(stillingPeriode) && stillingPeriode.gyldigFom().isBefore(tomorrow());
    }

    public static boolean isCurrentPeriod(OrgtilknytningState orgtilknytning) {
        return isCurrentOrFuturePeriod(orgtilknytning) && orgtilknytning.gyldigFom().isBefore(tomorrow());
    }

    public static boolean isHistoricPeriod(OrgtilknytningState orgtilknytning) {
        return orgtilknytning.gyldigTom().orElse(NO_END_DATE).isBefore(tomorrow());
    }

    public static boolean isHistoricPeriod(StillingPeriode stillingPeriode) {
        return stillingPeriode.gyldigTom().isBefore(tomorrow());
    }

    public static boolean isCurrentOrFuturePeriod(StillingPeriode stillingPeriode) {
        return stillingPeriode.gyldigTom().isAfter(yesterday());
    }

    public static boolean isCurrentOrFuturePeriod(OrgtilknytningState orgtilknytning) {
        return orgtilknytning.gyldigTom().isEmpty() || orgtilknytning.gyldigTom().get().isAfter(yesterday());
    }
    public static LocalDate yesterday() { return LocalDate.now().minusDays(1); }

    public static LocalDate tomorrow() { return LocalDate.now().plusDays(1); }

    public static boolean overlapsInterval(LocalDate start, LocalDate end, StillingPeriode stillingPeriode) {
        return stillingPeriode.gyldigFom().isBefore(dayAfter(end)) &&
            (stillingPeriode.gyldigTom() == null || stillingPeriode.gyldigTom().isAfter(dayBefore(start)));
    }

    private static LocalDate dayAfter(LocalDate localDate) {
        return localDate.plusDays(1);
    }

    private static LocalDate dayBefore(LocalDate localDate) {
        return localDate.minusDays(1);
    }
}
