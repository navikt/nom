package no.nav.nom.apps.gql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.gql.exception.NotRelevantForIntegrationTestingException;
import no.nav.nom.apps.gql.projections.OrgEnhetSearch;
import no.nav.nom.apps.gql.projections.OrgEnhetSearchFilter;
import no.nav.nom.apps.gql.projections.OrgEnheterSearch;
import no.nav.nom.apps.gql.projections.RessursSearch;
import no.nav.nom.apps.gql.projections.RessursSearchFilter;
import no.nav.nom.apps.gql.projections.RessurserSearch;
import no.nav.nom.apps.gql.resolvers.LederResolver;
import no.nav.nom.apps.gql.resolvers.OrgEnhetResolver;
import no.nav.nom.apps.gql.resolvers.OrganiseringResolver;
import no.nav.nom.apps.gql.resolvers.OrgtilknytningResolver;
import no.nav.nom.apps.gql.resolvers.RessursResolver;
import no.nav.nom.apps.gql.resolvers.SearchResolver;
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
import no.nav.nom.contract.nom.api.graphql.RessursWithMetadataResult;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public abstract class AbstractGraphQlController implements GraphQlControllerInterface {

    private final OrgEnhetResolver orgEnhetResolver;
    private final OrgtilknytningResolver orgtilknytningResolver;
    private final OrganiseringResolver organiseringResolver;
    private final LederResolver lederResolver;

    private final RessursResolver ressursResolver;
    private final SearchResolver searchResolver;

    private final GraphqlSupportInterface graphqlSupport;



    public AbstractGraphQlController(
        OrgEnhetResolver orgEnhetResolver,
        OrgtilknytningResolver orgtilknytningResolver,
        OrganiseringResolver organiseringResolver,
        LederResolver lederResolver,
        RessursResolver ressursResolver,
        SearchResolver searchResolver,
        GraphqlSupportInterface graphqlSupportInterface
    ) {
        this.orgEnhetResolver = orgEnhetResolver;
        this.orgtilknytningResolver = orgtilknytningResolver;
        this.organiseringResolver = organiseringResolver;
        this.lederResolver = lederResolver;
        this.ressursResolver = ressursResolver;
        this.searchResolver = searchResolver;
        this.graphqlSupport = graphqlSupportInterface;
    }

    @QueryMapping("orgEnhet")
    @Override
    public Optional<OrgEnhet> orgEnhet(@Argument("where") OrgEnhetSearch orgEnhetSearch, DataFetchingEnvironment environment) {
        var x = SecurityContextHolder.getContext().getAuthentication();
        if(x == null){
            throw new RuntimeException("Authentication is null!");
        }
        graphqlSupport.count(environment);
        return orgEnhetResolver.orgEnhet(orgEnhetSearch);
    }

    @QueryMapping("orgEnheter")
    @Override
    public List<OrgEnhetResult> orgEnheter(@Argument("where") OrgEnheterSearch orgEnheterSearch, DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        return orgEnhetResolver.orgEnheter(orgEnheterSearch);
    }

    @SchemaMapping(typeName = "OrgEnhet", field = "organiseringer")
    @Override
    public List<Organisering> organisering(
        OrgEnhet orgEnhet,
        @Argument("retning") OrganiseringRetning retning,
        @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg,
        DataFetchingEnvironment environment
    ) {
        graphqlSupport.count(environment);
        return organiseringResolver.organiseringer(orgEnhet, retning, periodiskUtvalg);
    }

    @SchemaMapping(typeName = "OrgEnhet", field = "ledere")
    @Override
    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        return lederResolver.leder(orgEnhet, periodiskUtvalg);
    }

    @SchemaMapping(typeName = "OrgEnhet", field = "remedyEnhetId")
    @Override
    public String remedyEnhetId(OrgEnhet orgEnhet, DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        return graphqlSupport.remedyEnhetId(orgEnhet.getId());
    }


    @SchemaMapping(typeName = "OrgEnhet", field = "orgTilknytninger")
    @Override
    public List<OrgTilknytning> orgTilknytninger(OrgEnhet orgEnhet, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        return orgtilknytningResolver.orgtilknytninger(orgEnhet, periodiskUtvalg);
    }


    @SchemaMapping(typeName = "Organisering", field = "orgEnhet")
    @Override
    public OrgEnhet orgEnhet(Organisering organisering, DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        return orgEnhetResolver.orgEnhet(organisering);
    }


    @QueryMapping("ressurs")
    @Override
    public Optional<Ressurs> ressurs(@Argument("where") RessursSearch search, DataFetchingEnvironment environment) {
        // Using @Argument("where") to tell Spring to bind the RessursSearch projected payload on the values below the "where" key.
        graphqlSupport.count(environment);
        return ressursResolver.ressurs(search);
    }

    @QueryMapping("ressurser")
    @Override
    public List<RessursResult> ressurser(@Argument("where") RessurserSearch search, DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        return ressursResolver.ressurser(search);
    }

    @QueryMapping("ressurserWithMetadata")
    @Override
    public List<RessursWithMetadataResult> ressurserWithMetadata(DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        throw new NotRelevantForIntegrationTestingException("Ikke implementert, pga ikke relevant for integrasjonstesting");
    }

    @SchemaMapping(typeName = "Ressurs", field = "orgTilknytninger")
    @Override
    public List<RessursOrgTilknytning> orgTilknytninger(
        Ressurs ressurs,
        @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg,
        DataFetchingEnvironment environment
    ) {
        graphqlSupport.count(environment);
        return orgtilknytningResolver.getOrgTilknytningerForRessurs(ressurs, periodiskUtvalg);
    }

    @SchemaMapping(typeName = "Ressurs", field = "lederFor")
    @Override
    public List<LederOrgEnhet> lederFor(Ressurs ressurs, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        return lederResolver.lederFor(ressurs, periodiskUtvalg);
    }

    @SchemaMapping(typeName = "Ressurs", field = "ledere")
    @Override
    public List<RessursLeder> ledere(Ressurs ressurs, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        return lederResolver.ledere(ressurs.getNavident(), periodiskUtvalg);
    }

    @BatchMapping(typeName = "Ressurs", field = "epost")
    @Override
    public Mono<Map<Ressurs, DataFetcherResult<String>>> epost(List<Ressurs> ressurser) {
        return Mono.fromSupplier(() -> graphqlSupport.epost(ressurser));
    }
    @QueryMapping("search")
    @Override
    public List<Object> search(
        @Argument("term") String term,
        @Argument("ressursFilter") RessursSearchFilter ressursFilter,
        @Argument("orgEnhetFilter") OrgEnhetSearchFilter orgEnhetFilter,
        DataFetchingEnvironment environment
    ) {
        graphqlSupport.count(environment);
        var ressurs = isNull(ressursFilter) ? RessursSearchFilter.alle() : ressursFilter;
        var orgenhet = isNull(orgEnhetFilter) ? OrgEnhetSearchFilter.alle() : orgEnhetFilter;
        return searchResolver.search(term, ressurs, orgenhet);
    }

    @QueryMapping("searchRessurs")
    @Override
    public List<Ressurs> searchRessurs(@Argument("term") String term, @Argument("filter") RessursSearchFilter filter, DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        var searchFilter = isNull(filter) ? RessursSearchFilter.alle() : filter;
        return searchResolver.searchRessurs(term, searchFilter);
    }

    @QueryMapping("searchOrgEnhet")
    @Override
    public List<OrgEnhet> searchOrgEnhet(@Argument("term") String term, @Argument("filter") OrgEnhetSearchFilter filter, DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        var searchFilter = isNull(filter) ? OrgEnhetSearchFilter.alle() : filter;
        return searchResolver.searchOrgenhet(term, searchFilter);
    }

    /*

        deprecations below

     */

    @Deprecated(forRemoval = true)
    @QueryMapping("organisasjonsenhet")
    @Override
    public Optional<OrgEnhet> organisasjonsenhet(@Argument("where") OrgEnhetSearch orgEnhetSearch, DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        return orgEnhet(orgEnhetSearch, environment);
    }

    @Deprecated(forRemoval = true)
    @QueryMapping("organisasjonsenheter")
    @Override
    public List<OrgEnhetResult> organisasjonsenheter(@Argument("where") OrgEnheterSearch orgEnheterSearch, DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        return orgEnheter(orgEnheterSearch, environment);
    }

    @Deprecated
    @SchemaMapping(typeName = "OrgEnhet", field = "leder")
    @Override
    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet, DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        return lederResolver.leder(orgEnhet, PeriodiskUtvalgArg.AKTIVE);
    }

    @Deprecated
    @SchemaMapping(typeName = "OrgEnhet", field = "koblinger")
    @Override
    public List<OrgEnhetsKobling> koblinger(OrgEnhet orgEnhet, DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        return orgtilknytningResolver.koblinger(orgEnhet);
    }

    @Deprecated
    @BatchMapping(typeName = "Ressurs", field = "orgTilknytning")
    @Override
    public Mono<Map<Ressurs, List<RessursOrgTilknytning>>> orgTilknytning(List<Ressurs> ressurser, DataFetchingEnvironment environment) {
        graphqlSupport.count(environment);
        record Pair(Ressurs ressurs, List<RessursOrgTilknytning> ressursOrgTilknytningList){}

        return Mono.fromSupplier(
            () -> ressurser.stream()
                .map(it -> new Pair(it, orgtilknytningResolver.getOrgTilknytningerForRessurs(it, PeriodiskUtvalgArg.AKTIVE)))
                .collect(Collectors.toMap(r -> r.ressurs, r -> r.ressursOrgTilknytningList))
        );
    }

}
