package no.nav.nom.apps.api.graphql.common.projections;

import org.springframework.data.web.ProjectedPayload;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

/**
 * Projected payload for binding the graphql input type RessurserSearch
 * Use resolvePersonidenter() to resolve the list of personidenter to search for.
 * Use resolveNavidenter() to resolve the list of navidenter to search for.
 */
@ProjectedPayload
public interface RessurserSearch {
    RessurserSearch EMPTY_SEARCH = emptyRessurserSearch();

    @Deprecated(forRemoval = true)
    Set<String> getPersonIdenter();

    Set<String> getPersonidenter();

    @Deprecated(forRemoval = true)
    Set<String> getNavIdenter();

    Set<String> getNavidenter();

    default Set<String> resolvePersonidenter() {
        Set<String> identer = new HashSet<>();
        if (getPersonidenter() != null) {
            identer.addAll(getPersonidenter());
        }
        if (getPersonIdenter() != null) {
            identer.addAll(getPersonIdenter());
        }
        return identer;
    }

    default Set<String> resolveNavidenter() {
        Set<String> identer = new HashSet<>();
        if (getNavidenter() != null) {
            identer.addAll(getNavidenter());
        }
        if (getNavIdenter() != null) {
            identer.addAll(getNavIdenter());
        }
        return identer;
    }

    static RessurserSearch emptyRessurserSearch() {
        return new RessurserSearch() {
            @Override
            public Set<String> getPersonIdenter() {
                return emptySet();
            }

            @Override
            public Set<String> getPersonidenter() {
                return emptySet();
            }

            @Override
            public Set<String> getNavIdenter() {
                return emptySet();
            }

            @Override
            public Set<String> getNavidenter() {
                return emptySet();
            }
        };
    }
}
