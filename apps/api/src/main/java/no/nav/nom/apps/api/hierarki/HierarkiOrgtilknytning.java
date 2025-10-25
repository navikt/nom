package no.nav.nom.apps.api.hierarki;

import no.nav.nom.contract.nom.state.OrgtilknytningState;

public class HierarkiOrgtilknytning {
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
}
