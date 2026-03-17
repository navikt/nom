package no.nav.nom.libs.gql.projections;

import no.nav.nom.contract.nom.NomNivaa;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

public class OrgEnheterSearch {
    private final Set<String> agressoIder;
    private final Set<String> orgEnhetsKategori;
    private final Set<String> nomNivaa;
    private final String orgNiv;  // Deprekert Jan 2026
    private final String agressoOrgNiv;
    private final Set<String> ider;

    public OrgEnheterSearch(Set<String> agressoIder, Set<String> orgenhetskategori, Set<String> nomNivaa, String orgNiv, String agressoOrgNiv, Set<String> ider) {
        this.agressoIder = agressoIder;
        this.orgEnhetsKategori = orgenhetskategori;
        this.nomNivaa = nomNivaa;
        this.orgNiv = orgNiv;
        this.agressoOrgNiv = agressoOrgNiv;
        this.ider = ider;
    }

    public Set<String> getAgressoIder() {
        return this.agressoIder;
    }

    public Set<String> getOrgEnhetsKategori() { return this.orgEnhetsKategori; }

    public Set<String> getNomNivaa() { return this.nomNivaa;}

    public String getOrgNiv() {
        return this.orgNiv;
    }

    public String agressoOrgNiv(){
        return this.agressoOrgNiv;
    }

    public Set<String> getIder() {
        return this.ider;
    }

    public Set<String> resolveAgressoIder() {
        if (getAgressoIder() == null) {
            return emptySet();
        }
        return getAgressoIder();
    }

    public String resolveAgressoOrgNiv() {
        if (agressoOrgNiv != null) { // orgNiv is deprecated
            return agressoOrgNiv;
        }
        return orgNiv;
    }

    public Set<String> resolveNomNivaa() {
        if (nomNivaa != null) {
            return nomNivaa;
        }
        if (orgEnhetsKategori != null) {
            return  orgEnhetsKategori;
        }
        return emptySet();
    }

    public Set<NomNivaa> resolveNomNivaaEnum() {
        return this.resolveNomNivaa()
            .stream()
            .map(NomNivaa::valueOf)
            .collect(Collectors.toSet());
    }

    public Set<String> resolveIder() {
        if (getIder() == null) {
            return emptySet();
        }
        return getIder();
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (OrgEnheterSearch) obj;
        return Objects.equals(this.agressoIder, that.agressoIder) &&
            Objects.equals(this.orgEnhetsKategori, that.orgEnhetsKategori) &&
            Objects.equals(this.agressoOrgNiv, that.agressoOrgNiv) &&
            Objects.equals(this.ider, that.ider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agressoIder, orgEnhetsKategori, agressoOrgNiv, ider);
    }

    @Override
    public String toString() {
        return "OrgEnheterSearch[" +
            "agressoIder=" + agressoIder + ", " +
            "orgenhetskategori=" + orgEnhetsKategori + ", " +
            "agressoOrgNiv=" + agressoOrgNiv + ", " +
            "IDer=" + ider + ']';
    }

    static OrgEnheterSearch emptyOrgEnheterSearch() {
        return new OrgEnheterSearch(emptySet(), emptySet(), emptySet(), null, null, emptySet());
    }

    public static OrgEnheterSearch EMPTY_SEARCH = emptyOrgEnheterSearch();
}
