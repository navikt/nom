package no.nav.nom.apps.api.graphql.common.projections;

import org.springframework.data.web.ProjectedPayload;

import java.util.Set;

import static java.util.Collections.emptySet;

/**
 * Projected payload for binding the graphql input type OrgEnheterSearch
 * Use resolveAgressoIder() to resolve the list of agressoIder to search for.
 */
@ProjectedPayload
public interface OrgEnheterSearch {
    OrgEnheterSearch EMPTY_SEARCH = emptyOrgEnheterSearch();
    Set<String> getAgressoIder();
    Set<String> getOrgEnhetsKategori();
    String getOrgNiv();

    default Set<String> resolveAgressoIder() {
        if(getAgressoIder() == null) {
            return emptySet();
        }
        return getAgressoIder();
    }

    default Set<String> resolveOrgEnhetsKategori() {
        if(getOrgEnhetsKategori() == null){
            return emptySet();
        }
        return getOrgEnhetsKategori();
    }

    static OrgEnheterSearch emptyOrgEnheterSearch() {
        return new OrgEnheterSearch() {
            @Override
            public Set<String> getAgressoIder() {
                return emptySet();
            }

            @Override
            public Set<String> getOrgEnhetsKategori() {
                return emptySet();
            }

            @Override
            public String getOrgNiv() {
                return null;
            }
        };
    }
}
