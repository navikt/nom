package no.nav.nom.apps.api.graphql.controller;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.common.projections.RessursSearch;
import no.nav.nom.apps.api.graphql.common.projections.RessurserSearch;
import no.nav.nom.apps.api.graphql.ressurs.LederResolver;
import no.nav.nom.apps.api.graphql.ressurs.OrgTilknytningResolver;
import no.nav.nom.apps.api.graphql.ressurs.RessursPropertyResolver;
import no.nav.nom.apps.api.graphql.ressurs.RessursResolver;
import no.nav.nom.contract.nom.api.graphql.LederOrgEnhet;
import no.nav.nom.contract.nom.api.graphql.PeriodiskUtvalgArg;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursLeder;
import no.nav.nom.contract.nom.api.graphql.RessursOrgTilknytning;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.api.graphql.RessursWithMetadataResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@Controller
@PreAuthorize("@securityUtil.checkAuth(authentication)")
public class GraphQLRessursController {

    private final RessursResolver ressursResolver;
    private final RessursPropertyResolver propertyResolver;
    private final LederResolver lederResolver;
    private final OrgTilknytningResolver orgTilknytningResolver;
    private final GraphQLFieldConsumptionCounter counter;

    @Autowired
    public GraphQLRessursController(
            RessursResolver ressursResolver,
            RessursPropertyResolver propertyResolver,
            LederResolver lederResolver,
            GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter,
            OrgTilknytningResolver orgTilknytningResolver
    ) {
        this.ressursResolver = ressursResolver;
        this.propertyResolver = propertyResolver;
        this.lederResolver = lederResolver;
        this.counter = graphQLFieldConsumptionCounter;
        this.orgTilknytningResolver = orgTilknytningResolver;
    }

    @QueryMapping("ressurs")
    public Optional<Ressurs> ressurs(@Argument("where") RessursSearch search, DataFetchingEnvironment dataFetchingEnvironment) {
        // Using @Argument("where") to tell Spring to bind the RessursSearch projected payload on the values below the "where" key.
        counter.count(dataFetchingEnvironment);
        return ressursResolver.ressurs(search);
    }

    @QueryMapping("ressurser")
    public List<RessursResult> ressurser(@Argument("where") RessurserSearch search, DataFetchingEnvironment dataFetchingEnvironment) {
        counter.count(dataFetchingEnvironment);
        return ressursResolver.ressurser(search);
    }

    @QueryMapping("ressurserWithMetadata")
    public List<RessursWithMetadataResult> ressurserWithMetadata(DataFetchingEnvironment dataFetchingEnvironment) {
        counter.count(dataFetchingEnvironment);
        return ressursResolver.ressurserWithMetadata();
    }

    @Deprecated
    @BatchMapping(typeName = "Ressurs", field = "orgTilknytning")
    public Mono<Map<Ressurs, List<RessursOrgTilknytning>>> orgTilknytning(List<Ressurs> ressurser) {
        return Mono.fromSupplier(() -> orgTilknytningResolver.getOrgTilknytning(ressurser));
    }

    @SchemaMapping(typeName = "Ressurs", field = "orgTilknytninger")
    public List<RessursOrgTilknytning> orgTilknytninger(Ressurs ressurs, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg) {
        return orgTilknytningResolver.getOrgTilknytningerForRessurs(ressurs, periodiskUtvalg);
    }

    @SchemaMapping(typeName = "Ressurs", field = "lederFor")
    public List<LederOrgEnhet> lederFor(Ressurs ressurs, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg) {
        return lederResolver.lederFor(ressurs, periodiskUtvalg);
    }

    @SchemaMapping(typeName = "Ressurs", field = "ledere")
    public List<RessursLeder> ledere(Ressurs ressurs, @Argument("utvalg") PeriodiskUtvalgArg periodiskUtvalg) {
        return lederResolver.ledere(ressurs.getNavIdent(), periodiskUtvalg);
    }

    @BatchMapping(typeName = "Ressurs", field = "epost")
    public Mono<Map<Ressurs, DataFetcherResult<String>>> epost(List<Ressurs> ressurser) {
        return Mono.fromSupplier(() -> propertyResolver.epost(ressurser));
    }
}
