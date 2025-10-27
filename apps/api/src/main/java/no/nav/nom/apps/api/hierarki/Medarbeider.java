package no.nav.nom.apps.api.hierarki;

import no.nav.nom.apps.api.ressurs.api.dto.MinimalRessurs;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Medarbeider {

    private RessursState backingRessurs;
    private final List<HierarkiOrgtilknytning> hierarkiOrgtilknytningList;
    private final HierarkiService.MapAccess mapAccess;
    private final List<HierarkiOrgLeder> lederList;

    Medarbeider(RessursState backingRessurs, HierarkiService.MapAccess mapAccess) {
        this.backingRessurs = backingRessurs;
        this.hierarkiOrgtilknytningList = new ArrayList<>();
        this.lederList = new ArrayList<>();
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
        return new MinimalRessurs(this.backingRessurs.navident(), this.backingRessurs.visningsnavn().getFormattedVisningsnavn());
    }

    public HierarkiOrgenhet orgenhet(LocalDate date) {
        return this.hierarkiOrgtilknytningList.stream()
            .filter(it -> it.backingState.erDagligOppfolging() && it.isActive(date))
            .findFirst()
            .map(HierarkiOrgtilknytning::getOrgEnhet)
            .orElse(null);
    }

    public List<HierarkiOrgenhet> lederFor(LocalDate date){
        return this.lederList.stream().filter(it -> it.isActive(date)).map(HierarkiOrgLeder::getEnhet).toList();
    }

    public Medarbeider leder(LocalDate date) {

        var orgenhet = this.hierarkiOrgtilknytningList.stream()
            .filter(it -> it.backingState.erDagligOppfolging() && it.isActive(date))
            .findFirst().map(HierarkiOrgtilknytning::getOrgEnhet).orElse(null);

        if (orgenhet == null) {
            return null;
        }

        var candidate = orgenhet.enhetLeder(date);

        if(candidate == null) {
            return null;
        }

        while(this.sameAs(candidate)){
            orgenhet = orgenhet.orgenhetOver(date);
            if(orgenhet == null){
                return candidate;
            }
            candidate = orgenhet.enhetLeder(date);
            if(candidate == null){
                return null;
            }
        }

        return candidate;
    }

    boolean sameAs(Medarbeider other) {
        return this.navIdent().equals(other.navIdent());
    }

    public NavIdent navIdent() {
        return NavIdent.create(this.backingRessurs);
    }

    /*
     * update funcs
     */

    void updateOrgtilknytning(OrgtilknytningState orgtilknytningState){
        // todo, also filter by FID etc
        this.hierarkiOrgtilknytningList.add(new HierarkiOrgtilknytning(orgtilknytningState, this.mapAccess));
    }


    public void updateLeder(OrgEnhetLederState lederState) {
        // todo, put HierarkiOrgLeder in map from backingstate.fid, then the instance is shared between medarbeider and orgenhet it concerns
        this.lederList.add(new HierarkiOrgLeder(lederState, this.mapAccess));
    }

    public void updateBackingState(RessursState ressursState) {
        this.backingRessurs = ressursState;
    }
}
