package no.nav.nom.contract.nom;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum Sektor{
    NAV_STATLIG,
    NAV_KOMMUNAL,
    EKSTERN;

    public boolean kanOppretteRessurs(){
        return this == EKSTERN || this == NAV_KOMMUNAL;
    }

    public boolean erStatlig() {
        return this == NAV_STATLIG;
    }
}
