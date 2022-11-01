package no.nav.nom.apps.api.graphql.common.projections;

import org.springframework.data.web.ProjectedPayload;

import java.util.Optional;

/**
 * Projected payload for binding the graphql input type PersonSearch
 * Use resolveIdent() to resolve the current personident
 */
@ProjectedPayload
public interface PersonSearch {

    @Deprecated(forRemoval = true)
    String getPersonIdent();
    String getPersonident();

    default boolean isPersonidentPresent() {
        return getPersonident() != null || getPersonIdent() != null;
    }

    default String resolvePersonident() {
        return getPersonident() != null ? getPersonident() : getPersonIdent();
    }

    default Optional<String> resolveIdent() {
        if (isPersonidentPresent()) {
            return Optional.of(resolvePersonident());
        }
        return Optional.empty();
    }
}
