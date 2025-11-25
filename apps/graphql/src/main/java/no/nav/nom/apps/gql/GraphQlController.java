package no.nav.nom.apps.gql;

import graphql.GraphQLException;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
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
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Controller
public class GraphQlController {

    private final OrgEnhetResolver orgEnhetResolver;
    private final OrgtilknytningResolver orgtilknytningResolver;
    private final OrganiseringResolver organiseringResolver;
    private final LederResolver lederResolver;
    private final RemedyIdResolverInterface remedyEnhetIdResolver;

    private final RessursResolver ressursResolver;
    private final RessursPropertyResolverInterface propertyResolver;
    private final SearchResolver searchResolver;

    private final FieldUsageCounterInterface counter;


    public GraphQlController(
        OrgEnhetResolver orgEnhetResolver,
        OrgtilknytningResolver orgtilknytningResolver,
        OrganiseringResolver organiseringResolver,
        LederResolver lederResolver,
        RemedyIdResolverInterface remedyEnhetIdResolver,
        RessursResolver ressursResolver,
        RessursPropertyResolverInterface propertyResolver,
        SearchResolver searchResolver, FieldUsageCounterInterface fieldUsageCounter
    ) {
        this.orgEnhetResolver = orgEnhetResolver;
        this.orgtilknytningResolver = orgtilknytningResolver;
        this.organiseringResolver = organiseringResolver;
        this.lederResolver = lederResolver;
        this.remedyEnhetIdResolver = remedyEnhetIdResolver;
        this.ressursResolver = ressursResolver;
        this.propertyResolver = propertyResolver;
        this.searchResolver = searchResolver;
        this.counter = fieldUsageCounter;
    }

    @QueryMapping("orgEnhet")
    public Optional<OrgEnhet> orgEnhet(@Argument("where") OrgEnhetSearch orgEnhetSearch, DataFetchingEnvironment environment) {
        counter.count(environment);
        return orgEnhetResolver.orgEnhet(orgEnhetSearch);
    }

    @QueryMapping("orgEnheter")
    public List<OrgEnhetResult> orgEnheter(@Argument("where") OrgEnheterSearch orgEnheterSearch, DataFetchingEnvironment environment) {
        counter.count(environment);
        return orgEnhetResolver.orgEnheter(orgEnheterSearch);
    }

    @SchemaMapping(typeName = "OrgEnhet", field = "organiseringer")
    public List<Organisering> organisering(OrgEnhet orgEnhet, @Argument("retning") OrganiseringRetning retning, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        counter.count(environment);
        return organiseringResolver.organiseringer(orgEnhet, retning, periodiskUtvalg);
    }

    @SchemaMapping(typeName = "OrgEnhet", field = "ledere")
    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        counter.count(environment);
        return lederResolver.leder(orgEnhet, periodiskUtvalg);
    }

    @SchemaMapping(typeName = "OrgEnhet", field = "remedyEnhetId")
    public String remedyEnhetId(OrgEnhet orgEnhet, DataFetchingEnvironment environment) {
        counter.count(environment);
        return remedyEnhetIdResolver.remedyEnhetId(orgEnhet.getId());
    }


    @SchemaMapping(typeName = "OrgEnhet", field = "orgTilknytninger")
    public List<OrgTilknytning> orgTilknytninger(OrgEnhet orgEnhet, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        counter.count(environment);
        return orgtilknytningResolver.orgtilknytninger(orgEnhet, periodiskUtvalg);
    }


    @SchemaMapping(typeName = "Organisering", field = "orgEnhet")
    public OrgEnhet orgEnhet(Organisering organisering, DataFetchingEnvironment environment) {
        counter.count(environment);
        return orgEnhetResolver.orgEnhet(organisering);
    }


    @QueryMapping("ressurs")
    public Optional<Ressurs> ressurs(@Argument("where") RessursSearch search, DataFetchingEnvironment environment) {
        // Using @Argument("where") to tell Spring to bind the RessursSearch projected payload on the values below the "where" key.
        counter.count(environment);
        return ressursResolver.ressurs(search);
    }

    @QueryMapping("ressurser")
    public List<RessursResult> ressurser(@Argument("where") RessurserSearch search, DataFetchingEnvironment environment) {
        counter.count(environment);
        return ressursResolver.ressurser(search);
    }

    @QueryMapping("ressurserWithMetadata")
    public List<RessursWithMetadataResult> ressurserWithMetadata(DataFetchingEnvironment environment) {
        counter.count(environment);

        throw new GraphQLException("Ikke implementert, pga ikke relevant for integrasjonstesting", null);
    }

    @SchemaMapping(typeName = "Ressurs", field = "orgTilknytninger")
    public List<RessursOrgTilknytning> orgTilknytninger(Ressurs ressurs, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        counter.count(environment);
        return orgtilknytningResolver.getOrgTilknytningerForRessurs(ressurs, periodiskUtvalg);
    }

    @SchemaMapping(typeName = "Ressurs", field = "lederFor")
    public List<LederOrgEnhet> lederFor(Ressurs ressurs, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        counter.count(environment);
        return lederResolver.lederFor(ressurs, periodiskUtvalg);
    }

    @SchemaMapping(typeName = "Ressurs", field = "ledere")
    public List<RessursLeder> ledere(Ressurs ressurs, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg, DataFetchingEnvironment environment) {
        counter.count(environment);
        return lederResolver.ledere(ressurs.getNavident(), periodiskUtvalg);
    }

    @BatchMapping(typeName = "Ressurs", field = "epost")
    public Mono<Map<Ressurs, DataFetcherResult<String>>> epost(List<Ressurs> ressurser) {
        return Mono.fromSupplier(() -> propertyResolver.epost(ressurser));
    }
    @QueryMapping("search")
    public List<Object> search(@Argument("term") String term, @Argument("ressursFilter") RessursSearchFilter ressursFilter, @Argument("orgEnhetFilter") OrgEnhetSearchFilter orgEnhetFilter, DataFetchingEnvironment environment) {
        counter.count(environment);
        var ressurs = isNull(ressursFilter) ? RessursSearchFilter.alle() : ressursFilter;
        var orgenhet = isNull(orgEnhetFilter) ? OrgEnhetSearchFilter.alle() : orgEnhetFilter;
        return searchResolver.search(term, ressurs, orgenhet);
    }

    @QueryMapping("searchRessurs")
    public List<Ressurs> searchRessurs(@Argument("term") String term, @Argument("filter") RessursSearchFilter filter, DataFetchingEnvironment environment) {
        counter.count(environment);
        var searchFilter = isNull(filter) ? RessursSearchFilter.alle() : filter;
        return searchResolver.searchRessurs(term, searchFilter);
    }

    @QueryMapping("searchOrgEnhet")
    public List<OrgEnhet> searchOrgEnhet(@Argument("term") String term, @Argument("filter") OrgEnhetSearchFilter filter, DataFetchingEnvironment environment) {
        counter.count(environment);
        var searchFilter = isNull(filter) ? OrgEnhetSearchFilter.alle() : filter;
        return searchResolver.searchOrgenhet(term, searchFilter);
    }

    /*

        deprecations below

     */

    @Deprecated(forRemoval = true) // NB! ikke fjern umiddelbart. Counter manglet - lagt inn 10.09
    @QueryMapping("organisasjonsenhet")
    public Optional<OrgEnhet> organisasjonsenhet(@Argument("where") OrgEnhetSearch orgEnhetSearch, DataFetchingEnvironment environment) {
        counter.count(environment);
        return orgEnhet(orgEnhetSearch, environment);
    }

    @Deprecated(forRemoval = true)  // NB! ikke fjern umiddelbart. Counter manglet - lagt inn 10.09
    @QueryMapping("organisasjonsenheter")
    public List<OrgEnhetResult> organisasjonsenheter(@Argument("where") OrgEnheterSearch orgEnheterSearch, DataFetchingEnvironment environment) {
        counter.count(environment);
        return orgEnheter(orgEnheterSearch, environment);
    }

    @Deprecated
    @SchemaMapping(typeName = "OrgEnhet", field = "leder")
    public List<OrgEnhetsLeder> leder(OrgEnhet orgEnhet, DataFetchingEnvironment environment) {
        counter.count(environment);
        return lederResolver.leder(orgEnhet, PeriodiskUtvalgArg.AKTIVE);
    }

    @Deprecated
    @SchemaMapping(typeName = "OrgEnhet", field = "koblinger")
    public List<OrgEnhetsKobling> koblinger(OrgEnhet orgEnhet, DataFetchingEnvironment environment) {
        counter.count(environment);
        return orgtilknytningResolver.koblinger(orgEnhet);
    }

    @Deprecated
    @BatchMapping(typeName = "Ressurs", field = "orgTilknytning")
    public Mono<Map<Ressurs, List<RessursOrgTilknytning>>> orgTilknytning(List<Ressurs> ressurser, DataFetchingEnvironment environment) {
        counter.count(environment);
        record Pair(Ressurs ressurs, List<RessursOrgTilknytning> ressursOrgTilknytningList){}

        return Mono.fromSupplier(
            () -> ressurser.stream()
                .map(it -> new Pair(it, orgtilknytningResolver.getOrgTilknytningerForRessurs(it, PeriodiskUtvalgArg.AKTIVE)))
                .collect(Collectors.toMap(r -> r.ressurs, r -> r.ressursOrgTilknytningList))
        );
    }

}
