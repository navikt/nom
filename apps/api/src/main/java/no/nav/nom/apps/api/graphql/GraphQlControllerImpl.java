package no.nav.nom.apps.api.graphql;

import graphql.GraphQLException;
import graphql.GraphqlErrorException;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.graphql.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.resolvers.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.graphql.resolvers.RessursPropertyResolver;
import no.nav.nom.apps.gql.FieldUsageCounterInterface;
import no.nav.nom.apps.gql.GraphQlController;
import no.nav.nom.apps.gql.RemedyIdResolverInterface;
import no.nav.nom.apps.gql.RessursPropertyResolverInterface;
import no.nav.nom.apps.gql.projections.OrgEnhetSearch;
import no.nav.nom.apps.gql.resolvers.LederResolver;
import no.nav.nom.apps.gql.resolvers.OrgEnhetResolver;
import no.nav.nom.apps.gql.resolvers.OrganiseringResolver;
import no.nav.nom.apps.gql.resolvers.OrgtilknytningResolver;
import no.nav.nom.apps.gql.resolvers.RessursResolver;
import no.nav.nom.apps.gql.resolvers.SearchResolver;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@PreAuthorize("@securityUtil.checkAuth(authentication)")
@Controller
public class GraphQlControllerImpl extends GraphQlController {

    private final SecurityUtil securityUtil;

    public GraphQlControllerImpl(
        OrgEnhetResolver orgEnhetResolver,
        OrgtilknytningResolver orgtilknytningResolver,
        OrganiseringResolver organiseringResolver,
        LederResolver lederResolver,
        RemedyIdResolverInterface remedyEnhetIdResolver4,
        RessursResolver ressursResolver,
        SearchResolver searchResolver,
        FieldUsageCounterInterface graphQLFieldConsumptionCounter,
        RessursPropertyResolverInterface ressursPropertyResolver,
        SecurityUtil securityUtil
    ) {
        super(
            orgEnhetResolver,
            orgtilknytningResolver,
            organiseringResolver,
            lederResolver,
            remedyEnhetIdResolver4,
            ressursResolver,
            ressursPropertyResolver,
            searchResolver,
            graphQLFieldConsumptionCounter
        );
        this.securityUtil = securityUtil;
    }

    @Override
    @PreAuthorize("@securityUtil.checkAuth(authentication)")
    public Optional<OrgEnhet> orgEnhet(OrgEnhetSearch orgEnhetSearch, DataFetchingEnvironment environment) {
        checkAuth(environment);
        return super.orgEnhet(orgEnhetSearch, environment);
    }

    private void checkAuth(DataFetchingEnvironment environment) {
        try{
            securityUtil.checkAuth(SecurityContextHolder.getContext().getAuthentication());
        } catch (RuntimeException ex){
            throw GraphqlErrorException.newErrorException().sourceLocation(environment.getSource()).message("Unauthorized").build();
        }


    }
}
