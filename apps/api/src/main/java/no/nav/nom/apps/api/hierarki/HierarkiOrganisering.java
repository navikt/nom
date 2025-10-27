package no.nav.nom.apps.api.hierarki;

import no.nav.nom.contract.nom.state.OrganiseringState;

import java.time.LocalDate;

class HierarkiOrganisering {

    private final OrganiseringState backingState;
    private final HierarkiService.MapAccess mapAccess;

    HierarkiOrganisering(OrganiseringState organiseringState, HierarkiService.MapAccess mapAccess) {
        this.backingState = organiseringState;
        this.mapAccess = mapAccess;
    }

    public boolean isActive(LocalDate date){
        var fom = this.backingState.gyldigFom();
        var tom = this.backingState.gyldigTom();
        var notBeforeStart = fom.minusDays(1).isBefore(date);
        var notAfterEnd = tom == null || tom.plusDays(1).isAfter(date);

        return notBeforeStart && notAfterEnd;
    }

    public HierarkiOrgenhet getEnhetOver(){
        return this.mapAccess.getOrgenhetMap().get(NomId.create(this.backingState.idOver()));
    }

    public HierarkiOrgenhet getEnhetUnder(){
        return this.mapAccess.getOrgenhetMap().get(NomId.create(this.backingState.id()));
    }


}
