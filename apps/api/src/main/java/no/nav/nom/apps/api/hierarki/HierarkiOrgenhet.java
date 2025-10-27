package no.nav.nom.apps.api.hierarki;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HierarkiOrgenhet {
    OrgEnhetState backingState;
    private final HierarkiService.MapAccess mapAccess;
    private List<HierarkiOrgtilknytning> medarbeiderOrgTilknytningList =  new ArrayList<>();

    private final List<HierarkiOrgLeder> orgEnhetLederStateList = new ArrayList<>();

    private final List<HierarkiOrganisering> organiseringUnderList = new ArrayList<>();
    private final List<HierarkiOrganisering> organiseringOppoverList = new ArrayList<>();

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

    public Medarbeider enhetLeder(LocalDate date) {
        var aktivLederKobling = this.orgEnhetLederStateList.stream().filter(it -> it.isActive(date)).findFirst();
        return aktivLederKobling.map(HierarkiOrgLeder::getLeder).orElse(null);
    }

    public List<Medarbeider> medarbeiderePaaEnhet() {
        return getMedarbeidereByCache();
        // return getMedarbeidereByLookup();
    }

    public HierarkiOrgenhet orgenhetOver(LocalDate date) {
        return this.organiseringOppoverList.stream().filter(x -> x.isActive(date)).findFirst().map(HierarkiOrganisering::getEnhetOver).orElse(null);
    }

    public List<HierarkiOrgenhet> orgenhetUnderList(LocalDate date) {
        return this.organiseringUnderList.stream().filter(x -> x.isActive(date)).map(HierarkiOrganisering::getEnhetUnder).toList();
    }


    /*
        update api for inside package
     */

    void updateBackingState(OrgEnhetState s) {
        this.backingState = s;
    }

    void updateOrgtilknytning(OrgtilknytningState orgtilknytningState){
        // todo, also filter by FID etc
        this.medarbeiderOrgTilknytningList.add(new HierarkiOrgtilknytning(orgtilknytningState, this.mapAccess));
    }

    void updateLeder(OrgEnhetLederState lederState){
        // todo, also filter by FID etc
        HierarkiOrgLeder n = new HierarkiOrgLeder(lederState, this.mapAccess);
        this.orgEnhetLederStateList.add(n);
    }

    void updateOrganisering(OrganiseringState organiseringState) {
        var thisId = this.backingState.id();
        switch(organiseringState){
            case OrganiseringState os when os.id().equals(thisId) -> {
                this.organiseringOppoverList.add(new HierarkiOrganisering(organiseringState,this.mapAccess));
            }
            case OrganiseringState os when os.idOver().equals(thisId) -> {
                this.organiseringUnderList.add(new HierarkiOrganisering(organiseringState, this.mapAccess));
            }
            default -> throw new IllegalArgumentException("ID mismatch. Organisering inneholder ikke enhet " + thisId);
        }
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
