package no.nav.nom.apps.api.hierarki;

import no.nav.nom.contract.nom.state.OrgEnhetLederState;

import java.time.LocalDate;

class HierarkiOrgLeder {
    private final OrgEnhetLederState backingState;
    private final HierarkiService.MapAccess mapAccess;

    public HierarkiOrgLeder(OrgEnhetLederState backingState, HierarkiService.MapAccess mapAccess) {
        this.backingState = backingState;
        this.mapAccess = mapAccess;
    }

    public Medarbeider getLeder(){
        var navident = NavIdent.create(this.backingState.navident());
        // assumtion is that `this` object wouldn't exist if IDs don't map to existing objects
        return this.mapAccess.getMedarbeiderMap().get(navident);
    }

    public HierarkiOrgenhet getEnhet(){
        var nomId = NomId.create(this.backingState.orgEnhetId());
        return this.mapAccess.getOrgenhetMap().get(nomId);
    }

    public boolean isActive(LocalDate date){
        var fom = this.backingState.startdato();
        var tom = this.backingState.sluttdato();
        var beforeStart =  date.isBefore(fom);
        var afterEnd = tom != null && date.isAfter(tom);

        return !beforeStart && !afterEnd;
    }



}
