package no.nav.nom.apps.api.common;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.state.OrgEnhetState;

import java.time.LocalDate;
import java.util.Optional;

public class OrgEnhetStateBuilder {
    String fid;
    String id;  // Ikke kryptisk uid; ment for menneskelig daglig bruk
    String navn;
    LocalDate gyldigFom;
    Optional<LocalDate> gyldigTom;
    Optional<String> overordnetOrgEnhetFid;
    Optional<String> lederFid;
    Optional<OrgEnhetsType> orgEnhetsType;
    Optional<NomNivaa> nomNivaa;
    Optional<String> uniqueAgressoId;
    Optional<String> agressoId;
    Optional<String> agressoOrgNivaa;
    Optional<String> agressoOrgenhetType;

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
        this.gyldigTom = Optional.ofNullable(gyldigTom);
        return this;
    }

    public OrgEnhetStateBuilder overordnetOrgEnhetFid(String overordnetOrgEnhetFid) {
        this.overordnetOrgEnhetFid = Optional.of(overordnetOrgEnhetFid);
        return this;
    }

    public OrgEnhetStateBuilder lederFid(String lederFid) {
        this.lederFid = Optional.of(lederFid);
        return this;
    }

    public OrgEnhetStateBuilder orgEnhetsType(OrgEnhetsType orgEnhetsType) {
        this.orgEnhetsType = Optional.of(orgEnhetsType);
        return this;
    }

    public OrgEnhetStateBuilder nomNivaa(NomNivaa nomNivaa) {
        this.nomNivaa = Optional.of(nomNivaa);
        return this;
    }

    public OrgEnhetStateBuilder uniqueAgressoId(String uniqueAgressoId) {
        this.uniqueAgressoId = Optional.of(uniqueAgressoId);
        return this;
    }

    public OrgEnhetStateBuilder agressoId(String agressoId) {
        this.agressoId = Optional.of(agressoId);
        return this;
    }

    public OrgEnhetStateBuilder agressoOrgNivaa(String agressoOrgNivaa) {
        this.agressoOrgNivaa = Optional.of(agressoOrgNivaa);
        return this;
    }

    public OrgEnhetStateBuilder agressoOrgenhetType(String agressoOrgenhetType) {
        this.agressoOrgenhetType = Optional.of(agressoOrgenhetType);
        return this;
    }

    public OrgEnhetStateBuilder(int i) {
        this.fid = String.valueOf(i);
        this.id = String.valueOf(i);
        this.navn = String.valueOf(i);
        this.gyldigFom = LocalDate.now();
        this.gyldigTom = Optional.of(LocalDate.now());
        this.overordnetOrgEnhetFid = Optional.empty();
        this.lederFid = Optional.empty();
        this.orgEnhetsType = Optional.empty();
        this.nomNivaa = Optional.empty();
        this.uniqueAgressoId = Optional.empty();
        this.agressoId = Optional.of(String.valueOf(i));
        this.agressoOrgNivaa = Optional.of("ORGENHET");
        this.agressoOrgenhetType = Optional.empty();
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
            this.agressoOrgenhetType
        );
    }
}
