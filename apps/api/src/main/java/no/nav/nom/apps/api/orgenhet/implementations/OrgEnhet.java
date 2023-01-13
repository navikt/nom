package no.nav.nom.apps.api.orgenhet.implementations;

import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.contract.nom.OrgEnhetsKategori;
import no.nav.nom.contract.nom.state.OrgEnhetState;

import java.time.LocalDate;
import java.util.Optional;

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
    public String getAgressoId() { return orgEnhetState.agressoId().orElse(null); }

    @Override
    public String getAgressoOrgenhetType() {
        return orgEnhetState.agressoOrgenhetType().orElse(null);
    }

    @Override
    public String getNavn() {
        return orgEnhetState.navn();
    }

    @Override
    public String getOrgNiv() {
        return orgEnhetState.agressoOrgNivaa().orElse(null);
    }

    @Override
    public LocalDate getGyldigFom() {
        return orgEnhetState.gyldigFom();
    }

    @Override
    public Optional<LocalDate> getGyldigTom() { return orgEnhetState.gyldigTom(); }

    @Override
    public Optional<OrgEnhetsKategori> getOrgEnhetsKategori() {
        return orgEnhetState.orgEnhetsKategori();
    }

    @Override
    public String getUniqueAgressoId() {
        return orgEnhetState.uniqueAgressoId().orElse(null);
    }
}
