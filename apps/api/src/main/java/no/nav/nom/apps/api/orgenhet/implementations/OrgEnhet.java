package no.nav.nom.apps.api.orgenhet.implementations;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.state.OrgEnhetState;

import java.time.LocalDate;

public class OrgEnhet implements IOrgenhet {

    private final OrgEnhetState orgEnhetState;

    public OrgEnhet(OrgEnhetState orgEnhetState) {
        this.orgEnhetState = orgEnhetState;
    }

    @Override
    public String getFid() {
        return orgEnhetState.fid();
    }

    @Override
    public String getId() { return orgEnhetState.id(); }

    @Override
    public String getAgressoId() { return orgEnhetState.agressoId(); }

    @Override
    public String getAgressoOrgenhetType() {
        return orgEnhetState.agressoOrgenhetType();
    }

    @Override
    public String getNavn() {
        return orgEnhetState.navn();
    }

    @Override
    public String getOrgNiv() {
        return orgEnhetState.agressoOrgNivaa();
    }

    @Override
    public LocalDate getGyldigFom() {
        return orgEnhetState.gyldigFom();
    }

    @Override
    public LocalDate getGyldigTom() { return orgEnhetState.gyldigTom(); }

    @Override
    public NomNivaa getNomNivaa() {
        return orgEnhetState.nomNivaa();
    }

    @Override
    public String getUniqueAgressoId() {
        return orgEnhetState.uniqueAgressoId();
    }

    @Override
    public OrgEnhetsType getOrgEnhetsType() {
        return orgEnhetState.orgEnhetsType();
    }

    public String getOverordnetOrgEnhetFid() {
        return orgEnhetState.overordnetOrgEnhetFid();
    }

    @Override
    public String getTilgangsenhetId() {
        return orgEnhetState.tilgangsenhetId();
    }
}
