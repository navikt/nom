package no.nav.nom.apps.api.hierarki;

import no.nav.nom.apps.api.ressurs.api.dto.MinimalRessurs;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;

import java.util.ArrayList;
import java.util.List;

public class Medarbeider {

    private final RessursState backingRessurs;
    private final List<HierarkiOrgtilknytning> hierarkiOrgtilknytningList;
    private final HierarkiService.MapAccess mapAccess;

    Medarbeider(RessursState backingRessurs, HierarkiService.MapAccess mapAccess) {
        this.backingRessurs = backingRessurs;
        this.hierarkiOrgtilknytningList = new ArrayList<>();
        this.mapAccess = mapAccess;
    }

    public boolean erInnunderOrgenhet(HierarkiOrgenhet orgenhet) {
        throw new RuntimeException("Not impl");
    }

    public boolean kanRedigere(Medarbeider medarbeider) {
        throw new RuntimeException("Not impl");
    }

    public boolean kanOppretteRessurserTilEnhet(HierarkiOrgenhet opprettelseKontekstOrgenhet) {
        throw new RuntimeException("Not impl");

    }

    public MinimalRessurs toMinimalRessurs() {
        return new MinimalRessurs(this.backingRessurs.navident(),this.backingRessurs.visningsnavn().getFormattedVisningsnavn());
    }

    public HierarkiOrgenhet orgenhet() {
        var medDol = this.hierarkiOrgtilknytningList.stream().filter(it -> it.backingState.erDagligOppfolging()).findFirst().get();
        return medDol.getOrgEnhet();

    }

    /* update funcs
     *
     *
     */

    void updateOrgtilknytning(OrgtilknytningState orgtilknytningState){
        // todo, also filter by FID etc
        this.hierarkiOrgtilknytningList.add(new HierarkiOrgtilknytning(orgtilknytningState, this.mapAccess));
    }
}
