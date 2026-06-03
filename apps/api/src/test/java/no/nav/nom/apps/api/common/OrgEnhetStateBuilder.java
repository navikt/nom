package no.nav.nom.apps.api.common;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.state.OrgEnhetState;

import java.time.LocalDate;

public class OrgEnhetStateBuilder {
    String fid;
    String id;  // Ikke kryptisk uid; ment for menneskelig daglig bruk
    String navn;
    LocalDate gyldigFom;
    LocalDate gyldigTom;
    String overordnetOrgEnhetFid;
    String lederFid;
    OrgEnhetsType orgEnhetsType;
    NomNivaa nomNivaa;
    String uniqueAgressoId;
    String agressoId;
    String agressoOrgNivaa;
    String agressoOrgenhetType;
    String tilgangsenhetId;

    public OrgEnhetStateBuilder fid(String fid) {
        this.fid = fid;
        return this;
    }

    public OrgEnhetStateBuilder id(String id) {
        this.id = id;
        return this;
    }

    public OrgEnhetStateBuilder navn(String navn) {
        this.navn = navn;
        return this;
    }

    public OrgEnhetStateBuilder gyldigFom(LocalDate gyldigFom) {
        this.gyldigFom = gyldigFom;
        return this;
    }

    public OrgEnhetStateBuilder gyldigTom(LocalDate gyldigTom) {
        this.gyldigTom = gyldigTom;
        return this;
    }

    public OrgEnhetStateBuilder overordnetOrgEnhetFid(String overordnetOrgEnhetFid) {
        this.overordnetOrgEnhetFid = overordnetOrgEnhetFid;
        return this;
    }

    public OrgEnhetStateBuilder lederFid(String lederFid) {
        this.lederFid = lederFid;
        return this;
    }

    public OrgEnhetStateBuilder orgEnhetsType(OrgEnhetsType orgEnhetsType) {
        this.orgEnhetsType = orgEnhetsType;
        return this;
    }

    public OrgEnhetStateBuilder nomNivaa(NomNivaa nomNivaa) {
        this.nomNivaa = nomNivaa;
        return this;
    }

    public OrgEnhetStateBuilder uniqueAgressoId(String uniqueAgressoId) {
        this.uniqueAgressoId = uniqueAgressoId;
        return this;
    }

    public OrgEnhetStateBuilder agressoId(String agressoId) {
        this.agressoId = agressoId;
        return this;
    }

    public OrgEnhetStateBuilder agressoOrgNivaa(String agressoOrgNivaa) {
        this.agressoOrgNivaa = agressoOrgNivaa;
        return this;
    }

    public OrgEnhetStateBuilder agressoOrgenhetType(String agressoOrgenhetType) {
        this.agressoOrgenhetType = agressoOrgenhetType;
        return this;
    }

    public OrgEnhetStateBuilder tilgangsenhetId(String tilgangsenhetId) {
        this.tilgangsenhetId = tilgangsenhetId;
        return this;
    }

    public OrgEnhetStateBuilder(int i) {
        this.fid = String.valueOf(i);
        this.id = String.valueOf(i);
        this.navn = String.valueOf(i);
        this.gyldigFom = LocalDate.now();
        this.gyldigTom = LocalDate.now();
        this.overordnetOrgEnhetFid = null;
        this.lederFid = null;
        this.orgEnhetsType = null;
        this.nomNivaa = null;
        this.uniqueAgressoId = null;
        this.agressoId = String.valueOf(i);
        this.agressoOrgNivaa = "ORGENHET";
        this.agressoOrgenhetType = null;
    }

    public OrgEnhetState build() {
        return new OrgEnhetState(
            this.fid,
            this.id,
            this.navn,
            this.gyldigFom,
            this.gyldigTom,
            this.overordnetOrgEnhetFid,
            this.lederFid,
            this.orgEnhetsType,
            this.nomNivaa,
            this.uniqueAgressoId,
            this.agressoId,
            this.agressoOrgNivaa,
            this.agressoOrgenhetType,
            this.tilgangsenhetId
        );
    }
}
