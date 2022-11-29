package no.nav.nom.apps.api.graphql.common.projections;

import org.springframework.data.web.ProjectedPayload;

import java.util.Optional;

/**
 * Projected payload for binding the graphql input type RessursSearch
 * Use resolveIdent() to resolve the current navident, and then the personident
 */
@ProjectedPayload
public interface RessursSearch extends PersonSearch {

    @Deprecated(forRemoval = true)
    String getNavIdent();

    String getNavident();


    default boolean isNavidentPresent() {
        return getNavIdent() != null || getNavident() != null;
    }

    default String resolveNavident() {
        return getNavident() != null ? getNavident() : getNavIdent();
    }

    @Override
    default Optional<String> resolveIdent() {
        if (isNavidentPresent()) {
            return Optional.of(resolveNavident());
        } else if (isPersonidentPresent()) {
            return Optional.of(resolvePersonident());
        }
        return Optional.empty();
    }
}
