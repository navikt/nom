package no.nav.nom.libs.gql.resolvers;

import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;

import java.time.LocalDate;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNullElse;
import static no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg.AKTIVE;

public class Util {

    public static <T> boolean filterByPeriodiskUtvalg(T periodisertObject, PeriodiskUtvalgArg periodiskUtvalg) {
        if (periodiskUtvalg == null) periodiskUtvalg = AKTIVE; // Default
        return switch (periodiskUtvalg) {
            case ALLE -> true;
            case AKTIVE -> switch (periodisertObject) {
                case OrganiseringState o -> PeriodeUtil.isCurrentPeriod(o);
                case OrgEnhetLederState o -> PeriodeUtil.isCurrentPeriod(o);
                case OrgtilknytningState o -> PeriodeUtil.isCurrentPeriod(o);
                case LederResolver.LederForRessurs o -> PeriodeUtil.isCurrentPeriod(o);
                case OrgEnhetState o -> PeriodeUtil.isCurrentPeriod(o);
                case RessursState r -> PeriodeUtil.isCurrentPeriod(r);
                default -> throwError(periodisertObject);
            };
            case FREMTIDIGE -> switch (periodisertObject) {
                case OrganiseringState o -> PeriodeUtil.isFuturePeriod(o);
                case OrgEnhetLederState o -> PeriodeUtil.isFuturePeriod(o);
                case OrgtilknytningState o -> PeriodeUtil.isFuturePeriod(o);
                case LederResolver.LederForRessurs o -> PeriodeUtil.isFuturePeriod(o);
                case OrgEnhetState o -> PeriodeUtil.isFuturePeriod(o);
                case RessursState r -> PeriodeUtil.isFuturePeriod(r);
                default -> throwError(periodisertObject);
            };
            case HISTORISKE -> switch (periodisertObject) {
                case OrganiseringState o -> PeriodeUtil.isHistoricPeriod(o);
                case OrgEnhetLederState o -> PeriodeUtil.isHistoricPeriod(o);
                case OrgtilknytningState o -> PeriodeUtil.isHistoricPeriod(o);
                case LederResolver.LederForRessurs o -> PeriodeUtil.isHistoricPeriod(o);
                case OrgEnhetState o -> PeriodeUtil.isHistoricPeriod(o);
                case RessursState r -> PeriodeUtil.isHistoricPeriod(r);
                default -> throwError(periodisertObject);
            };
        };
    }

    private static <T> boolean throwError(T obj) {
        throw new IllegalArgumentException("PeriodiskUtvalg fra feilet - kan ikke håndtere typen %s som periodisert objekt".formatted(obj.getClass().getName()));
    }

    private static class PeriodeUtil {

        public static boolean isCurrentPeriod(OrganiseringState organisering) {
            return isCurrentOrFuturePeriod(organisering) && organisering.gyldigFom().isBefore(tomorrow());
        }

        public static boolean isCurrentPeriod(OrgEnhetLederState leder) {
            return isCurrentOrFuturePeriod(leder) && leder.startdato().isBefore(tomorrow());
        }


        public static boolean isCurrentPeriod(OrgtilknytningState orgtilknytning) {
            return isCurrentOrFuturePeriod(orgtilknytning) && orgtilknytning.gyldigFom().isBefore(tomorrow());
        }

        public static boolean isCurrentPeriod(LederResolver.LederForRessurs leder) {
            return isCurrentOrFuturePeriod(leder) && leder.startdato().isBefore(tomorrow());
        }

        public static boolean isCurrentPeriod(OrgEnhetState orgEnhet) {
            return isCurrentOrFuturePeriod(orgEnhet) && orgEnhet.gyldigFom().isBefore(tomorrow());
        }

        public static boolean isCurrentPeriod(RessursState ressurs) {
            return isCurrentOrFuturePeriod(ressurs) && (ressurs.startdato() == null || ressurs.startdato().isBefore(tomorrow()));
        }

        public static boolean isFuturePeriod(OrgtilknytningState orgtilknytning) {
            return orgtilknytning.gyldigFom().isAfter(LocalDate.now());
        }

        public static boolean isFuturePeriod(OrganiseringState organisering) {
            return organisering.gyldigFom().isAfter(LocalDate.now());
        }

        public static boolean isFuturePeriod(OrgEnhetLederState leder) {
            return leder.startdato().isAfter(LocalDate.now());
        }

        public static boolean isFuturePeriod(LederResolver.LederForRessurs leder) {
            return leder.startdato().isAfter(LocalDate.now());
        }

        public static boolean isFuturePeriod(OrgEnhetState orgEnhet) {
            return orgEnhet.gyldigFom().isAfter(LocalDate.now());
        }

        public static boolean isFuturePeriod(RessursState ressurs) { return ressurs.startdato() != null && ressurs.startdato().isAfter(LocalDate.now()); }

        public static final LocalDate NO_END_DATE = LocalDate.of(2099, 12, 31);

        public static boolean isHistoricPeriod(OrganiseringState organisering) {
            return requireNonNullElse(organisering.gyldigTom(), NO_END_DATE).isBefore(tomorrow());
        }

        public static boolean isHistoricPeriod(OrgtilknytningState orgtilknytning) {
            return requireNonNullElse(orgtilknytning.gyldigTom(), NO_END_DATE).isBefore(tomorrow());
        }

        public static boolean isHistoricPeriod(OrgEnhetLederState leder){
            return requireNonNullElse(leder.sluttdato(), NO_END_DATE).isBefore(tomorrow());
        }

        public static boolean isHistoricPeriod(LederResolver.LederForRessurs leder){
            return requireNonNullElse(leder.sluttdato(), NO_END_DATE).isBefore(tomorrow());
        }

        public static boolean isHistoricPeriod(OrgEnhetState orgEnhet){
            return requireNonNullElse(orgEnhet.gyldigTom(), NO_END_DATE).isBefore(tomorrow());
        }

        public static boolean isHistoricPeriod(RessursState ressurs) { return requireNonNullElse(ressurs.sluttdato(), NO_END_DATE).isBefore(tomorrow()); }

        public static boolean isCurrentOrFuturePeriod(OrganiseringState organisering) {
            return isNull(organisering.gyldigTom()) || organisering.gyldigTom().isAfter(yesterday());
        }

        public static boolean isCurrentOrFuturePeriod(OrgtilknytningState orgtilknytning) {
            return isNull(orgtilknytning.gyldigTom()) || orgtilknytning.gyldigTom().isAfter(yesterday());
        }

        public static boolean isCurrentOrFuturePeriod(OrgEnhetLederState leder) {
            return isNull(leder.sluttdato()) || leder.sluttdato().isAfter(yesterday());
        }

        public static boolean isCurrentOrFuturePeriod(LederResolver.LederForRessurs leder) {
            return isNull(leder.sluttdato()) || leder.sluttdato().isAfter(yesterday());
        }

        public static boolean isCurrentOrFuturePeriod(OrgEnhetState orgEnhet) {
            return isNull(orgEnhet.gyldigTom()) || orgEnhet.gyldigTom().isAfter(yesterday());
        }

        public static boolean isCurrentOrFuturePeriod(RessursState ressurs) {
            return isNull(ressurs.sluttdato()) || ressurs.sluttdato().isAfter(yesterday());
        }

        public static LocalDate today() { return LocalDate.now(); }
        public static LocalDate yesterday() { return today().minusDays(1); }
        public static LocalDate tomorrow() { return today().plusDays(1); }

        public static LocalDate dayAfter(LocalDate localDate) { return localDate.plusDays(1); }

        public static LocalDate dayBefore(LocalDate localDate) { return localDate.minusDays(1); }

    }
}
