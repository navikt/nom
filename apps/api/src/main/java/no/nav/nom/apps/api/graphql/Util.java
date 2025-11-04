package no.nav.nom.apps.api.graphql;

import no.nav.nom.apps.api.orgenhetleder.LederForRessurs;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;

import static no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg.AKTIVE;

public class Util {

    // TODO -  PeriodeUtil burde implementere metodene basert på f.eks. Periodisert interface

    public static <T> boolean filterByPeriodiskUtvalg(T periodisertObject, PeriodiskUtvalgArg periodiskUtvalg) {
        if (periodiskUtvalg == null) periodiskUtvalg = AKTIVE; // Default
        return switch (periodiskUtvalg) {
            case ALLE -> true;
            case AKTIVE -> switch (periodisertObject) {
                case OrganiseringState o -> PeriodeUtil.isCurrentPeriod(o);
                case OrgEnhetLederState o -> PeriodeUtil.isCurrentPeriod(o);
                case OrgtilknytningState o -> PeriodeUtil.isCurrentPeriod(o);
                case LederForRessurs o -> PeriodeUtil.isCurrentPeriod(o);
                default -> throwError(periodisertObject);
            };
            case FREMTIDIGE -> switch (periodisertObject) {
                case OrganiseringState o -> PeriodeUtil.isFuturePeriod(o);
                case OrgEnhetLederState o -> PeriodeUtil.isFuturePeriod(o);
                case OrgtilknytningState o -> PeriodeUtil.isFuturePeriod(o);
                case LederForRessurs o -> PeriodeUtil.isFuturePeriod(o);
                default -> throwError(periodisertObject);
            };
            case HISTORISKE -> switch (periodisertObject) {
                case OrganiseringState o -> PeriodeUtil.isHistoricPeriod(o);
                case OrgEnhetLederState o -> PeriodeUtil.isHistoricPeriod(o);
                case OrgtilknytningState o -> PeriodeUtil.isHistoricPeriod(o);
                case LederForRessurs o -> PeriodeUtil.isHistoricPeriod(o);
                default -> throwError(periodisertObject);
            };
        };
    }

    private static <T> boolean throwError(T obj) {
        throw new IllegalArgumentException("PeriodiskUtvalg fra feilet - kan ikke håndtere typen %s som periodisert objekt".formatted(obj.getClass().getName()));
    }
}
