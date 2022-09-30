package no.nav.nom.apps.api.graphql.common.projections;

import org.springframework.data.web.ProjectedPayload;

import java.util.Set;

import static java.util.Collections.emptySet;

@ProjectedPayload
public interface OrgEnheterSearch {
    Set<String> getagressoIder();
    String getOrgNiv();

    default Set<String> resolveAgressoIder() {
        if(getagressoIder() == null) {
            return emptySet();
        }
        return getagressoIder();
    }
}
