package no.nav.nom.contract.nom;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum Sektor{
    NAV_STATLIG,
    NAV_KOMMUNAL,
    EKSTERN;

    public boolean kanOppretteRessurs(){
        return this.erEkstern() || this.erKommunal();
    }

    public boolean erStatlig() {
        return this == NAV_STATLIG;
    }

    public boolean erKommunal() {
        return this == NAV_KOMMUNAL;
    }

    public boolean erEkstern() {
        return this == EKSTERN;
    }
}
