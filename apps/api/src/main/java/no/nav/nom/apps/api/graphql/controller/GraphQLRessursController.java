package no.nav.nom.apps.api.graphql.controller;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.common.projections.RessursSearch;
import no.nav.nom.apps.api.graphql.common.projections.RessurserSearch;
import no.nav.nom.apps.api.graphql.common.resolver.FolkeregisterPersonResolver;
import no.nav.nom.apps.api.graphql.ressurs.LederResolver;
import no.nav.nom.apps.api.graphql.ressurs.OrgTilknytningResolver;
import no.nav.nom.apps.api.graphql.ressurs.RessursPropertyResolver;
import no.nav.nom.apps.api.graphql.ressurs.RessursResolver;
import no.nav.nom.apps.api.graphql.ressurs.TelefonResolver;
import no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson;
import no.nav.nom.contract.nom.api.graphql.LederOrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursRequest;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursResponse;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursLeder;
import no.nav.nom.contract.nom.api.graphql.RessursOrgTilknytning;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.api.graphql.Telefon;
import org.dataloader.BatchLoaderEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.nav.nom.apps.api.graphql.common.projections.RessurserSearch.EMPTY_SEARCH;


@Controller
@PreAuthorize("isAuthenticated()")
public class GraphQLRessursController {

    private final RessursResolver ressursResolver;
    private final RessursPropertyResolver propertyResolver;
    private final TelefonResolver telefonResolver;
    private final LederResolver lederResolver;
    private final OrgTilknytningResolver orgTilknytningResolver;
    private final FolkeregisterPersonResolver personResolver;
    private final GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter;

    @Autowired
    public GraphQLRessursController(
            RessursResolver ressursResolver,
            RessursPropertyResolver propertyResolver,
            TelefonResolver telefonResolver,
            LederResolver lederResolver,
            GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter,
            OrgTilknytningResolver orgTilknytningResolver,
            FolkeregisterPersonResolver personResolver
    ) {
        this.ressursResolver = ressursResolver;
        this.propertyResolver = propertyResolver;
        this.telefonResolver = telefonResolver;
        this.lederResolver = lederResolver;
        this.graphQLFieldConsumptionCounter = graphQLFieldConsumptionCounter;
        this.orgTilknytningResolver = orgTilknytningResolver;
        this.personResolver = personResolver;
    }

    @QueryMapping("ressurs")
    public Optional<Ressurs> ressurs(@Argument("where") RessursSearch search, DataFetchingEnvironment environment) {
        // Using @Argument("where") to tell Spring to bind the RessursSearch projected payload on the values below the "where" key.
        graphQLFieldConsumptionCounter.count(environment);
        return ressursResolver.ressurs(search);
    }

    @QueryMapping("ressurser")
    public List<RessursResult> ressurser(@Argument("where") Optional<RessurserSearch> search, DataFetchingEnvironment environment) {
        graphQLFieldConsumptionCounter.count(environment);
        return ressursResolver.ressurser(search.orElse(EMPTY_SEARCH));
    }

    @MutationMapping("opprettRessurs")
    public DataFetcherResult<OpprettRessursResponse> opprettRessurs(@Argument("request") OpprettRessursRequest request, DataFetchingEnvironment environment, @AuthenticationPrincipal JwtAuthenticationToken token) {
        return ressursResolver.opprettRessurs(request, environment, token);
    }

    @Deprecated
    @BatchMapping(typeName = "Ressurs", field = "koblinger")
    public Mono<Map<Ressurs, List<RessursOrgTilknytning>>> koblinger(List<Ressurs> ressurser) {
        return orgTilknytning(ressurser);
    }

    @BatchMapping(typeName = "Ressurs", field = "orgTilknytning")
    public Mono<Map<Ressurs, List<RessursOrgTilknytning>>> orgTilknytning(List<Ressurs> ressurser) {
        return Mono.fromSupplier(() -> orgTilknytningResolver.getOrgTilknytning(ressurser));
    }

    @BatchMapping(typeName = "Ressurs", field = "lederFor")
    public Mono<Map<Ressurs, List<LederOrgEnhet>>> lederFor(List<Ressurs> ressurser) {
        return Mono.fromSupplier(() -> lederResolver.load(ressurser, lederResolver::lederFor));
    }

    @BatchMapping(typeName = "Ressurs", field = "ledere")
    public Mono<Map<Ressurs, List<RessursLeder>>> ledere(List<Ressurs> ressurser) {
        return Mono.fromSupplier(() -> lederResolver.load(ressurser, lederResolver::ledere));
    }

    @Deprecated
    @BatchMapping(typeName = "Ressurs", field = "person")
    public Mono<Map<Ressurs, DataFetcherResult<FolkeregisterPerson>>> person(List<Ressurs> ressurser, BatchLoaderEnvironment environment) {
        return folkeregisterPerson(ressurser, environment);
    }

    @BatchMapping(typeName = "Ressurs", field = "folkeregisterPerson")
    public Mono<Map<Ressurs, DataFetcherResult<FolkeregisterPerson>>> folkeregisterPerson(List<Ressurs> ressurser, BatchLoaderEnvironment environment) {
        return Mono.fromSupplier(() -> personResolver.folkeregisterPerson(ressurser, Ressurs::getPersonIdent, environment));
    }

    @BatchMapping(typeName = "Ressurs", field = "epost")
    public Mono<Map<Ressurs, DataFetcherResult<String>>> epost(List<Ressurs> ressurser, BatchLoaderEnvironment environment) {
        return Mono.fromSupplier(() -> propertyResolver.epost(ressurser, environment));
    }

    @Deprecated(forRemoval = true)
    @BatchMapping(typeName = "Ressurs", field = "visningsNavn")
    public Mono<Map<Ressurs, DataFetcherResult<String>>> visningsNavn(List<Ressurs> ressurser, BatchLoaderEnvironment environment) {
        return visningsnavn(ressurser, environment);
    }

    @BatchMapping(typeName = "Ressurs", field = "visningsnavn")
    public Mono<Map<Ressurs, DataFetcherResult<String>>> visningsnavn(List<Ressurs> ressurser, BatchLoaderEnvironment environment) {
        return Mono.fromSupplier(() -> propertyResolver.visningsnavn(ressurser, environment));
    }

    @BatchMapping(typeName = "Ressurs", field = "fornavn")
    public Mono<Map<Ressurs, DataFetcherResult<String>>> fornavn(List<Ressurs> ressurser, BatchLoaderEnvironment environment) {
        return Mono.fromSupplier(() -> propertyResolver.fornavn(ressurser, environment));
    }

    @BatchMapping(typeName = "Ressurs", field = "etternavn")
    public Mono<Map<Ressurs, DataFetcherResult<String>>> etternavn(List<Ressurs> ressurser, BatchLoaderEnvironment environment) {
        return Mono.fromSupplier(() -> propertyResolver.etternavn(ressurser, environment));
    }

    @BatchMapping(typeName = "Ressurs", field = "telefon")
    public Mono<Map<Ressurs, List<Telefon>>> telefon(List<Ressurs> ressurser) {
        return Mono.fromSupplier(() -> telefonResolver.load(ressurser));
    }

}
