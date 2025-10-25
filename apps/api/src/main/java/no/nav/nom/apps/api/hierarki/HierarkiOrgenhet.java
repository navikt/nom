package no.nav.nom.apps.api.hierarki;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;

import java.util.ArrayList;
import java.util.List;

public class HierarkiOrgenhet {
    private final OrgEnhetState backingState;
    private final HierarkiService.MapAccess mapAccess;
    private List<HierarkiOrgtilknytning> medarbeiderOrgTilknytningList =  new ArrayList<>();
    private Medarbeider leder = null;

    HierarkiOrgenhet(OrgEnhetState s, HierarkiService.MapAccess mapAccess) {
        this.backingState = s;
        this.mapAccess = mapAccess;
        medarbeiderOrgTilknytningList = new ArrayList<>();
    }


    /*
    api for outside of package
     */

    public IOrgenhet transformToIOrgenhet() {
        throw new RuntimeException("Not impl");
    }

    public List<Medarbeider> medarbeidereMedRedigeringsTilgang() {
        throw new RuntimeException("Not implemented");
    }

    public boolean hasNomNivaa(NomNivaa nomNivaa) {
        throw new RuntimeException("Not implemented");
    }

    public boolean isBetweenDriftOgArbeidsOmraade() {
        throw new RuntimeException("Not implemented");
    }

    public NomId id(){
        return NomId.create(this.backingState.id());
    }

    public Medarbeider enhetLeder() {
        return this.leder;
    }

    public List<Medarbeider> medarbeiderePaaEnhet() {
        return getMedarbeidereByCache();
//        return getMedarbeidereByLookup();
    }


    /*
        update api for inside package
     */
    void updateOrgtilknytning(OrgtilknytningState orgtilknytningState){
        // todo, also filter by FID etc
        this.medarbeiderOrgTilknytningList.add(new HierarkiOrgtilknytning(orgtilknytningState, this.mapAccess));
    }

    void updateLeder(Medarbeider nyLeder){
        this.leder = nyLeder;
    }

    private List<Medarbeider> getMedarbeidereByLookup() {
        var orgtilknytningMap = this.mapAccess.getOrgtilknytningMap();
        var relevantOt = orgtilknytningMap.values().stream().filter(it -> it.backingState.orgEnhetId().equals(this.backingState.id()));
        var id = relevantOt.map(it -> it.backingState.navident()).map(NavIdent::create);
        return id.map(it -> this.mapAccess.getMedarbeiderMap().get(it)).toList();
    }

    private List<Medarbeider> getMedarbeidereByCache() {
        return this.medarbeiderOrgTilknytningList.stream().map(HierarkiOrgtilknytning::getMedarbeider).toList();
    }


}
