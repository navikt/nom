package no.nav.nom.libs.gql.projections;

import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;

public record RessurserSearchFilter(
    SektorSelection gjeldendeSektorSelection,
    IdentTypeSelection identTypeSelection,
    PeriodiskUtvalgArg periodiskUtvalg,
    String fysiskBrukersted,
    String tilgangsenhetId
) {

    public enum IdentTypeSelection {
        ROBOT,
        STANDARD,
        VIKAFOSSEN,
        UTENLANDSK_KONSULENT,
        UKJENT,
        ALLE
    }

    static RessurserSearchFilter alle() {
        return new RessurserSearchFilter(SektorSelection.ALLE, IdentTypeSelection.ALLE, PeriodiskUtvalgArg.ALLE, null, null);
    }

    public static RessurserSearchFilter ALLE = alle();
}
