package no.nav.nom.apps.gql.projections;

import org.springframework.data.web.ProjectedPayload;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

/**
 * Projected payload for binding the graphql input type RessurserSearch
 */
@ProjectedPayload
public interface RessurserSearch {
    RessurserSearch EMPTY_SEARCH = emptyRessurserSearch();

    Set<String> getPersonidenter();

    Set<String> getNavidenter();

    static RessurserSearch emptyRessurserSearch() {
        return new RessurserSearch() {
            @Override
            public Set<String> getPersonidenter() {
                return emptySet();
            }

            @Override
            public Set<String> getNavidenter() {
                return emptySet();
            }
        };
    }
}
