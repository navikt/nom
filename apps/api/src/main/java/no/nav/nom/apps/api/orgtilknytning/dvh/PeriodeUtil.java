package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.contract.nom.state.OrgtilknytningState;

import java.time.LocalDate;
import java.util.Optional;

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

    public static LocalDate today() { return LocalDate.now(); }

    public static LocalDate yesterday() { return today().minusDays(1); }

    public static LocalDate tomorrow() { return today().plusDays(1); }

    public static boolean overlapsInterval(StillingPeriode stillingPeriode1, StillingPeriode stillingPeriode2) {
        return overlapsInterval(stillingPeriode1.gyldigFom(), stillingPeriode1.gyldigTom(), stillingPeriode2);
    }

    public static boolean overlapsInterval(OrgtilknytningState orgtilknytning1, OrgtilknytningState orgtilknytning2) {
        return overlapsInterval(orgtilknytning1.gyldigFom(), orgtilknytning1.gyldigTom().orElse(NO_END_DATE), orgtilknytning2);
    }

    public static boolean overlapsInterval(LocalDate start, LocalDate end, StillingPeriode stillingPeriode) {
        return stillingPeriode.gyldigFom().isBefore(dayAfter(end)) &&
            (stillingPeriode.gyldigTom() == null || stillingPeriode.gyldigTom().isAfter(dayBefore(start)));
    }

    public static boolean overlapsInterval(LocalDate start, LocalDate end, OrgtilknytningState orgtilknytning) {
        return orgtilknytning.gyldigFom().isBefore(dayAfter(end)) && orgtilknytning.gyldigTom().orElse(NO_END_DATE).isAfter(dayBefore(start));
    }

    public static LocalDate dayAfter(LocalDate localDate) { return localDate.plusDays(1); }

    public static LocalDate dayBefore(LocalDate localDate) { return localDate.minusDays(1); }

    public static boolean lik(LocalDate dato, LocalDate sjekkMotDato) {
        dato = dato == null ? NO_END_DATE : dato;
        return dato.equals(sjekkMotDato == null ? NO_END_DATE : sjekkMotDato);
    }

    public static boolean foer(LocalDate dato, LocalDate sjekkMotDato) {
        dato = dato == null ? NO_END_DATE : dato;
        return dato.isBefore(sjekkMotDato == null ? NO_END_DATE : sjekkMotDato);
    }

    public static boolean etter(LocalDate dato, LocalDate sjekkMotDato) {
        dato = dato == null ? NO_END_DATE : dato;
        return dato.isAfter(sjekkMotDato == null ? NO_END_DATE : sjekkMotDato);
    }

    public static boolean likEllerFoer(LocalDate dato, LocalDate sjekkMotDato) {
        dato = dato == null ? NO_END_DATE : dato;
        return dato.isBefore(dayAfter(sjekkMotDato == null ? NO_END_DATE : sjekkMotDato));
    }

    public static boolean likEllerEtter(LocalDate dato, LocalDate sjekkMotDato) {
        dato = dato == null ? NO_END_DATE : dato;
        return dato.isAfter(dayBefore(sjekkMotDato == null ? NO_END_DATE : sjekkMotDato));
    }
}
