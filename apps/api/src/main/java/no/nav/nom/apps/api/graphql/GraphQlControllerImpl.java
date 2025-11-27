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
import no.nav.nom.apps.gql.projections.OrgEnheterSearch;
import no.nav.nom.apps.gql.projections.RessursSearch;
import no.nav.nom.apps.gql.projections.RessurserSearch;
import no.nav.nom.apps.gql.resolvers.LederResolver;
import no.nav.nom.apps.gql.resolvers.OrgEnhetResolver;
import no.nav.nom.apps.gql.resolvers.OrganiseringResolver;
import no.nav.nom.apps.gql.resolvers.OrgtilknytningResolver;
import no.nav.nom.apps.gql.resolvers.RessursResolver;
import no.nav.nom.apps.gql.resolvers.SearchResolver;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.OrgTilknytning;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.api.graphql.RessursWithMetadataResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
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
    public Optional<OrgEnhet> orgEnhet(OrgEnhetSearch orgEnhetSearch, DataFetchingEnvironment environment) {
        return super.orgEnhet(orgEnhetSearch, environment);
    }

    @Override
    public List<OrgEnhetResult> orgEnheter(OrgEnheterSearch orgEnheterSearch, DataFetchingEnvironment environment) {
        return super.orgEnheter(orgEnheterSearch, environment);
    }

    @Override
    public List<OrgTilknytning> orgTilknytninger(OrgEnhet orgEnhet, PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        return super.orgTilknytninger(orgEnhet, periodiskUtvalg, environment);
    }

    @Override
    public OrgEnhet orgEnhet(Organisering organisering, DataFetchingEnvironment environment) {
        return super.orgEnhet(organisering, environment);
    }

    @Override
    public List<RessursWithMetadataResult> ressurserWithMetadata(DataFetchingEnvironment environment) {
        return super.ressurserWithMetadata(environment);
    }

    @Override
    public List<RessursResult> ressurser(RessurserSearch search, DataFetchingEnvironment environment) {
        return super.ressurser(search, environment);
    }

    @Override
    public Optional<Ressurs> ressurs(RessursSearch search, DataFetchingEnvironment environment) {
        return super.ressurs(search, environment);
    }

    private void checkAuth() {
        securityUtil.checkAuth(SecurityContextHolder.getContext().getAuthentication());

    }
}
