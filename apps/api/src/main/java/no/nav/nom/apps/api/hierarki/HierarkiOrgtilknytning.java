package no.nav.nom.apps.api.hierarki;

import no.nav.nom.contract.nom.state.OrgtilknytningState;

import java.time.LocalDate;

class HierarkiOrgtilknytning {
    final OrgtilknytningState backingState;

    private final HierarkiService.MapAccess mapAccess;

    HierarkiOrgtilknytning(OrgtilknytningState backingState, HierarkiService.MapAccess mapAccess) {
        this.backingState = backingState;
        this.mapAccess = mapAccess;
    }

    public Medarbeider getMedarbeider() {
        var navident = NavIdent.create(backingState.navident());
        // assumtion is that `this` object wouldn't exist if IDs don't map to existing objects
        return this.mapAccess.getMedarbeiderMap().get(navident);
    }

    public HierarkiOrgenhet getOrgEnhet(){
        var nomId = NomId.create(backingState.orgEnhetId());
        // assumtion is that `this` object wouldn't exist if IDs don't map to existing objects
        return this.mapAccess.getOrgenhetMap().get(nomId);
    }

    public boolean isActive(LocalDate date) {
        var fom = this.backingState.gyldigFom();
        var tom = this.backingState.gyldigTom();
        var beforeStart = date.isBefore(fom);
        var afterEnd = tom != null && date.isAfter(tom);
        return !beforeStart && !afterEnd;
    }

    public boolean isDol(){
        return this.backingState.erDagligOppfolging();
    }
}
