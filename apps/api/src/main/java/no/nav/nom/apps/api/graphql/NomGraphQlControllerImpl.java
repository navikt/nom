package no.nav.nom.apps.api.graphql;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.contract.nom.api.graphql.LederOrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.OrgTilknytning;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import no.nav.nom.contract.nom.api.graphql.OrganiseringRetning;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursLeder;
import no.nav.nom.contract.nom.api.graphql.RessursOrgTilknytning;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.libs.gql.AbstractNomGraphQlController;
import no.nav.nom.libs.gql.NomGraphQlControllerInterface;
import no.nav.nom.libs.gql.NomGraphqlSupportInterface;
import no.nav.nom.libs.gql.projections.OrgEnhetSearch;
import no.nav.nom.libs.gql.projections.OrgEnhetSearchFilter;
import no.nav.nom.libs.gql.projections.OrgEnheterSearch;
import no.nav.nom.libs.gql.projections.OrgEnheterSearchFilter;
import no.nav.nom.libs.gql.projections.RessursSearch;
import no.nav.nom.libs.gql.projections.RessursSearchFilter;
import no.nav.nom.libs.gql.projections.RessurserSearch;
import no.nav.nom.libs.gql.projections.RessurserSearchFilter;
import no.nav.nom.libs.gql.resolvers.LederResolver;
import no.nav.nom.libs.gql.resolvers.OrgEnhetResolver;
import no.nav.nom.libs.gql.resolvers.OrganiseringResolver;
import no.nav.nom.libs.gql.resolvers.OrgtilknytningResolver;
import no.nav.nom.libs.gql.resolvers.RessursResolver;
import no.nav.nom.libs.gql.resolvers.SearchResolver;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller("nomGraphqlController")
@PreAuthorize("@securityUtil.checkAuth(authentication)")
public class NomGraphQlControllerImpl implements NomGraphQlControllerInterface {

    private final AbstractNomGraphQlController delegate;

    public NomGraphQlControllerImpl(
        OrgEnhetResolver orgEnhetResolver,
        OrgtilknytningResolver orgtilknytningResolver,
        OrganiseringResolver organiseringResolver,
        LederResolver lederResolver,
        RessursResolver ressursResolver,
        SearchResolver searchResolver,
        NomGraphqlSupportInterface graphqlSupportInterface
    ) {
        this.delegate = new AbstractNomGraphQlController(
            orgEnhetResolver,
            orgtilknytningResolver,
            organiseringResolver,
            lederResolver,
            ressursResolver,
            searchResolver,
            graphqlSupportInterface
        ) {};
    }

    @Override
    public Optional<OrgEnhet> orgEnhet(OrgEnhetSearch orgEnhetSearch, DataFetchingEnvironment environment) {
        return delegate.orgEnhet(orgEnhetSearch, environment);
    }

    @Override
    public List<OrgEnhetResult> orgEnheter(OrgEnheterSearch orgEnheterSearch, OrgEnheterSearchFilter orgEnheterSearchFilter, DataFetchingEnvironment environment) {
        return delegate.orgEnheter(orgEnheterSearch, orgEnheterSearchFilter, environment);
    }

    @Override
    public List<Organisering> organisering(
        OrgEnhet orgEnhet,
        OrganiseringRetning retning,
        PeriodiskUtvalgArg periodiskUtvalg,
        DataFetchingEnvironment environment
    ) {
        return delegate.organisering(orgEnhet, retning, periodiskUtvalg, environment);
    }

    @Override
    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet, PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        return delegate.leder(orgEnhet, periodiskUtvalg, environment);
    }

    @Override
    public String remedyEnhetId(OrgEnhet orgEnhet, DataFetchingEnvironment environment) {
        return delegate.remedyEnhetId(orgEnhet, environment);
    }

    @Override
    public List<OrgTilknytning> orgTilknytninger(OrgEnhet orgEnhet, PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        return delegate.orgTilknytninger(orgEnhet, periodiskUtvalg, environment);
    }

    @Override
    public OrgEnhet orgEnhet(Organisering organisering, DataFetchingEnvironment environment) {
        return delegate.orgEnhet(organisering, environment);
    }


    @Override
    public List<RessursOrgTilknytning> orgTilknytninger(Ressurs ressurs, PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        return delegate.orgTilknytninger(ressurs, periodiskUtvalg, environment);
    }

    @Override
    public List<LederOrgEnhet> lederFor(Ressurs ressurs, PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        return delegate.lederFor(ressurs, periodiskUtvalg, environment);
    }

    @Override
    public List<RessursLeder> ledere(Ressurs ressurs, PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        return delegate.ledere(ressurs, periodiskUtvalg, environment);
    }

    @Override
    public Mono<Map<Ressurs, DataFetcherResult<String>>> epost(List<Ressurs> ressurser) {
        return delegate.epost(ressurser);
    }

    @Override
    public List<Object> search(String term, RessursSearchFilter ressursFilter, OrgEnhetSearchFilter orgEnhetFilter, DataFetchingEnvironment environment) {
        return delegate.search(term, ressursFilter, orgEnhetFilter, environment);
    }

    @Override
    public List<Ressurs> searchRessurs(String term, RessursSearchFilter filter, DataFetchingEnvironment environment) {
        return delegate.searchRessurs(term, filter, environment);
    }

    @Override
    public List<OrgEnhet> searchOrgEnhet(String term, OrgEnhetSearchFilter filter, DataFetchingEnvironment environment) {
        return delegate.searchOrgEnhet(term, filter, environment);
    }

    @Override
    @Deprecated
    public Optional<OrgEnhet> organisasjonsenhet(OrgEnhetSearch orgEnhetSearch, DataFetchingEnvironment environment) {
        return delegate.orgEnhet(orgEnhetSearch, environment);
    }

    @Override
    @Deprecated
    public List<OrgEnhetResult> organisasjonsenheter(OrgEnheterSearch orgEnheterSearch, DataFetchingEnvironment environment) {
        return delegate.orgEnheter(orgEnheterSearch, OrgEnheterSearchFilter.ALLE, environment);
    }

    @Override
    @Deprecated
    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet, DataFetchingEnvironment environment) {
        return delegate.leder(orgEnhet, environment);
    }

    @Override
    public List<OrgEnhetsKobling> koblinger(OrgEnhet orgEnhet, DataFetchingEnvironment environment) {
        return delegate.koblinger(orgEnhet, environment);
    }

    @Override
    public List<RessursOrgTilknytning> orgTilknytning(Ressurs ressurs, DataFetchingEnvironment environment) {
        return delegate.orgTilknytning(ressurs, environment);
    }

    @Override
    public List<RessursResult> ressurser(RessurserSearch search, RessurserSearchFilter filter, DataFetchingEnvironment environment) {
        return delegate.ressurser(search, filter, environment);
    }

    @Override
    public Optional<Ressurs> ressurs(RessursSearch search, DataFetchingEnvironment environment) {
        return delegate.ressurs(search, environment);
    }

}
