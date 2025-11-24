package no.nav.nom.apps.gql.projections;

import org.springframework.data.web.ProjectedPayload;

import java.util.Optional;

/**
 * Projected payload for binding the graphql input type RessursSearch
 * Use resolveIdent() to resolve the current navident, and then the personident
 */
@ProjectedPayload
public interface RessursSearch {

    String getNavident();

    String getTelefonNummer();

    String getPersonident();

}
