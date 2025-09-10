package no.nav.nom.apps.api.graphql.common.projections;

import org.springframework.data.web.ProjectedPayload;

import java.util.Optional;

/**
 * Projected payload for binding the graphql input type RessursSearch
 * Use resolveIdent() to resolve the current navident, and then the personident
 */
@ProjectedPayload
public interface RessursSearch {

    @Deprecated(forRemoval = true)
    String getNavIdent();

    String getNavident();

    String getTelefonNummer();

    @Deprecated(forRemoval = true)
    String getPersonIdent();

    String getPersonident();

    default boolean isPersonidentPresent() {
        return getPersonident() != null || getPersonIdent() != null;
    }

    default String resolvePersonident() {
        return getPersonident() != null ? getPersonident() : getPersonIdent();
    }

    default boolean isNavidentPresent() {
        return getNavIdent() != null || getNavident() != null;
    }

    default String resolveNavident() {
        return getNavident() != null ? getNavident() : getNavIdent();
    }

    default Optional<String> resolveIdent() {
        if (isNavidentPresent()) {
            return Optional.of(resolveNavident());
        } else if (isPersonidentPresent()) {
            return Optional.of(resolvePersonident());
        }
        return Optional.empty();
    }
}
