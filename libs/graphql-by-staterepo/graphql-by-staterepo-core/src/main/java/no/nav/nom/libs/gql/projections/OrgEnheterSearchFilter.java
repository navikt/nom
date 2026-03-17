package no.nav.nom.libs.gql.projections;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;

import java.util.Set;

import static java.util.Collections.emptySet;

public record OrgEnheterSearchFilter(
    LovnodeSelection lovnodeSelection,
    OrganiseringSelection organiseringSelection,
    String organisertUnderOrgEnhetId,
    Set<NomNivaa> nomNivaaSelection,
    PeriodiskUtvalgArg periodiskUtvalg
) {
    public OrgEnheterSearchFilter {
        if (nomNivaaSelection == null) {
            nomNivaaSelection = emptySet();
        }
    }

    public enum OrganiseringSelection {
        UNDER_TOPPNODE_NAV,
        UNDER_HELFO,
        UNDER_SKATT,
        UNDER_RIKSREVISJONEN,
        UNDER_VIKAFOSSEN,
        UNDER_ANGITT_ORGENHET,
        ALLE
    }

    public enum LovnodeSelection {
        LOVNODE_ENHET,
        IKKE_LOVNODE_ENHET,
        ALLE
    }

    static OrgEnheterSearchFilter alle() {
        return new OrgEnheterSearchFilter(LovnodeSelection.ALLE, OrganiseringSelection.ALLE, null,  emptySet(), PeriodiskUtvalgArg.ALLE);
    }

    public static OrgEnheterSearchFilter ALLE = alle();
}
