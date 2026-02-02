package no.nav.nom.libs.gql.projections;

import no.nav.nom.contract.nom.NomNivaa;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

public class OrgEnheterSearch {
    private final Set<String> agressoIder;
    private final Set<String> orgenhetskategori;
    private final String orgNiv;  // Deprekert Jan 2026
    private final String agressoOrgNiv;
    private final Set<String> ider;

    public OrgEnheterSearch(Set<String> agressoIder, Set<String> orgenhetskategori, String orgNiv, String agressoOrgNiv, Set<String> ider) {
        this.agressoIder = agressoIder;
        this.orgenhetskategori = orgenhetskategori;
        this.orgNiv = orgNiv;
        this.agressoOrgNiv = agressoOrgNiv;
        this.ider = ider;
    }

    public Set<String> getAgressoIder() {
        return this.agressoIder;
    }

    public Set<String> getOrgEnhetsKategori() {
        return this.orgenhetskategori;
    }

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

    public Set<String> resolveNomNivaa() {
        if (getOrgEnhetsKategori() == null) {
            return emptySet();
        }
        return getOrgEnhetsKategori();
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
            Objects.equals(this.orgenhetskategori, that.orgenhetskategori) &&
            Objects.equals(this.agressoOrgNiv, that.agressoOrgNiv) &&
            Objects.equals(this.ider, that.ider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agressoIder, orgenhetskategori, agressoOrgNiv, ider);
    }

    @Override
    public String toString() {
        return "OrgEnheterSearch[" +
            "agressoIder=" + agressoIder + ", " +
            "orgenhetskategori=" + orgenhetskategori + ", " +
            "agressoOrgNiv=" + agressoOrgNiv + ", " +
            "IDer=" + ider + ']';
    }

    static OrgEnheterSearch emptyOrgEnheterSearch() {
        return new OrgEnheterSearch(emptySet(), emptySet(), null, null, emptySet());
    }

    public static OrgEnheterSearch EMPTY_SEARCH = emptyOrgEnheterSearch();
}
