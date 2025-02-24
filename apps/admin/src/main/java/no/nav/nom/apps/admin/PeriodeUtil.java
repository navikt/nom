package no.nav.nom.apps.admin;

import no.nav.nom.contract.nom.state.OrgtilknytningState;

import java.time.LocalDate;

// TODO Trekke ut denne og tilsvarende klasse i nom-api til libs/utils-test
public class PeriodeUtil {
    
    public static final LocalDate NO_END_DATE = LocalDate.of(2099, 12, 31);

    public static LocalDate today() { return LocalDate.now(); }

    public static LocalDate yesterday() { return today().minusDays(1); }

    public static LocalDate tomorrow() { return today().plusDays(1); }
    
    public static boolean isCurrentOrFuturePeriod(OrgtilknytningState orgtilknytning) {
        return orgtilknytning.gyldigTom().isEmpty() || orgtilknytning.gyldigTom().get().isAfter(yesterday());
    }

    public static boolean overlapsInterval(OrgtilknytningState orgtilknytning1, OrgtilknytningState orgtilknytning2) {
        return overlapsInterval(orgtilknytning1.gyldigFom(), orgtilknytning1.gyldigTom().orElse(NO_END_DATE), orgtilknytning2);
    }

    public static boolean overlapsInterval(LocalDate start, LocalDate end, OrgtilknytningState orgtilknytning) {
        return orgtilknytning.gyldigFom().isBefore(dayAfter(end)) && orgtilknytning.gyldigTom().orElse(NO_END_DATE).isAfter(dayBefore(start));
    }

    public static LocalDate dayAfter(LocalDate localDate) {
        return localDate.plusDays(1);
    }

    public static LocalDate dayBefore(LocalDate localDate) {
        return localDate.minusDays(1);
    }

    public static boolean likEllerEtter(LocalDate dato, LocalDate sjekkMotDato) {
        dato = dato == null ? NO_END_DATE : dato;
        return dato.isAfter(dayBefore(sjekkMotDato == null ? NO_END_DATE : sjekkMotDato));
    }
    
    public static boolean innenforSisteSeksMaaneder(LocalDate dato) {
        return likEllerEtter(dato, today().minusMonths(6));
    }
    
}
