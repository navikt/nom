package no.nav.nom.contract.nom.api.graphql;

public enum Sektor {
    NAV_STATLIG,
    NAV_KOMMUNAL,
    EKSTERN;

    public static Sektor from(no.nav.nom.contract.nom.eventsource.Sektor sektor) {
        return switch (sektor) {
            case NAV_KOMMUNAL -> NAV_KOMMUNAL;
            case NAV_STATLIG -> NAV_STATLIG;
            case EKSTERN -> EKSTERN;
        };
    }

    public static no.nav.nom.contract.nom.eventsource.Sektor toEventStateSektor(Sektor sektor){
        return switch (sektor) {
            case NAV_KOMMUNAL -> no.nav.nom.contract.nom.eventsource.Sektor.NAV_KOMMUNAL;
            case NAV_STATLIG -> no.nav.nom.contract.nom.eventsource.Sektor.NAV_STATLIG;
            case EKSTERN -> no.nav.nom.contract.nom.eventsource.Sektor.EKSTERN;
        };
    }
}
