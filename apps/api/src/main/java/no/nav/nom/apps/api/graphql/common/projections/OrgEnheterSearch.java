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
    Set<String> getagressoIder();
    String getOrgNiv();

    default Set<String> resolveAgressoIder() {
        if(getagressoIder() == null) {
            return emptySet();
        }
        return getagressoIder();
    }

    static OrgEnheterSearch emptyOrgEnheterSearch() {
        return new OrgEnheterSearch() {
            @Override
            public Set<String> getagressoIder() {
                return emptySet();
            }

            @Override
            public String getOrgNiv() {
                return null;
            }
        };
    }
}
