package no.nav.nom.apps.api.graphql.controller;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.common.resolver.FolkeregisterPersonResolver;
import no.nav.nom.apps.api.graphql.ressurs.LederResolver;
import no.nav.nom.apps.api.graphql.ressurs.OrgTilknytningResolver;
import no.nav.nom.apps.api.graphql.ressurs.RessursResolver;
import no.nav.nom.apps.api.graphql.common.projections.RessursSearch;
import no.nav.nom.apps.api.graphql.common.projections.RessurserSearch;
import no.nav.nom.apps.api.graphql.ressurs.TelefonResolver;
import no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson;
import no.nav.nom.contract.nom.api.graphql.LederOrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursRequest;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursResponse;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursLeder;
import no.nav.nom.contract.nom.api.graphql.RessursOrgTilknytning;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.api.graphql.Sektor;
import no.nav.nom.contract.nom.api.graphql.Telefon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;


@Controller
@SchemaMapping(typeName = "Ressurs")
public class GraphQLRessursController {

    private final RessursResolver ressursResolver;
    private final TelefonResolver telefonResolver;
    private final LederResolver lederResolver;
    private final OrgTilknytningResolver orgTilknytningResolver;

    private final FolkeregisterPersonResolver personResolver;
    private final GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter;

    @Autowired
    public GraphQLRessursController(
            RessursResolver ressursResolver,
            TelefonResolver telefonResolver,
            LederResolver lederResolver,
            GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter,
            OrgTilknytningResolver orgTilknytningResolver,
            FolkeregisterPersonResolver personResolver
    ) {
        this.ressursResolver = ressursResolver;
        this.telefonResolver = telefonResolver;
        this.lederResolver = lederResolver;
        this.graphQLFieldConsumptionCounter = graphQLFieldConsumptionCounter;
        this.orgTilknytningResolver = orgTilknytningResolver;
        this.personResolver = personResolver;
    }

    @QueryMapping
    public Optional<Ressurs> ressurs(@Argument("where") RessursSearch search, DataFetchingEnvironment environment) {
        // Using @Argument("where") to tell Spring to bind the RessursSearch projected payload on the values below the "where" key.
        graphQLFieldConsumptionCounter.count(environment);
        return ressursResolver.ressurs(search);
    }

    @QueryMapping
    public List<RessursResult> ressurser(@Argument("where") RessurserSearch search, DataFetchingEnvironment environment) {
        graphQLFieldConsumptionCounter.count(environment);
        return ressursResolver.ressurser(search);
    }

    @MutationMapping
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

    @SchemaMapping(typeName = "Ressurs", field = "lederFor")
    public List<LederOrgEnhet> lederFor(Ressurs ressurs) {
        return lederResolver.lederFor(ressurs);
    }

    @SchemaMapping(typeName = "Ressurs", field = "ledere")
    public List<RessursLeder> ledere(Ressurs ressurs) {
        return lederResolver.ledere(ressurs);
    }

    @Deprecated
    @BatchMapping(typeName = "Ressurs", field = "person")
    public Mono<Map<Ressurs, DataFetcherResult<FolkeregisterPerson>>> person(List<Ressurs> ressurser) {
        return folkeregisterPerson(ressurser);
    }

    @BatchMapping(typeName = "Ressurs", field = "folkeregisterPerson")
    public Mono<Map<Ressurs, DataFetcherResult<FolkeregisterPerson>>> folkeregisterPerson(List<Ressurs> ressurser) {
        // TODO: Implement batch loading
        return Mono.fromSupplier(() -> personResolver.folkeregisterPerson(ressurser, Ressurs::getPersonIdent));
    }

    @SchemaMapping(typeName = "Ressurs", field = "epost")
    public Optional<String> epost(Ressurs ressurs) {
        return Optional.empty();
    }

    @SchemaMapping(typeName = "Ressurs", field = "visningsNavn")
    public Optional<String> visningsNavn(Ressurs ressurs) {
        return visningsnavn(ressurs);
    }

    @SchemaMapping(typeName = "Ressurs", field = "visningsnavn")
    public Optional<String> visningsnavn(Ressurs ressurs) {
        return Optional.empty();
    }

    @SchemaMapping(typeName = "Ressurs", field = "fornavn")
    public Optional<String> fornavn(Ressurs ressurs) {
        return Optional.empty();
    }

    @SchemaMapping(typeName = "Ressurs", field = "etternavn")
    public Optional<String> etternavn(Ressurs ressurs) {
        return Optional.empty();
    }

    @SchemaMapping(typeName = "Ressurs", field = "telefon")
    public List<Telefon> telefon(Ressurs ressurs) {
        return telefonResolver.telefon(ressurs);
    }

    @Deprecated
    @SchemaMapping(typeName = "Ressurs", field = "ressurstype")
    public List<Sektor> ressurstype(Ressurs ressurs) {
        return sektor(ressurs);
    }

    @SchemaMapping(typeName = "Ressurs", field = "sektor")
    public List<Sektor> sektor(Ressurs ressurs) {
        return emptyList();
    }

    @Deprecated
    @SchemaMapping(typeName = "Ressurs", field = "sluttdato")
    public Optional<LocalDate> sluttdato(Ressurs ressurs) {
        return Optional.empty();
    }

}

