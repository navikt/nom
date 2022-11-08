package no.nav.nom.contract.nom.api.graphql;

import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.state.RessursState;

public enum Sektor {
    NAV_STATLIG,
    NAV_KOMMUNAL,
    EKSTERN;

    public static Sektor from(RessursState.Sektor sektor) {
        return switch (sektor) {
            case NAV_KOMMUNAL -> NAV_KOMMUNAL;
            case NAV_STATLIG -> NAV_STATLIG;
            case EKSTERN -> EKSTERN;
        };
    }

    public static RessursOpprettetEventV1.RessursRegisteredEventData.Sektor toEventStateSektor(Sektor sektor){
        return switch (sektor) {
            case NAV_KOMMUNAL -> RessursOpprettetEventV1.RessursRegisteredEventData.Sektor.NAV_KOMMUNAL;
            case NAV_STATLIG -> RessursOpprettetEventV1.RessursRegisteredEventData.Sektor.NAV_STATLIG;
            case EKSTERN -> RessursOpprettetEventV1.RessursRegisteredEventData.Sektor.EKSTERN;
        };
    }
}
