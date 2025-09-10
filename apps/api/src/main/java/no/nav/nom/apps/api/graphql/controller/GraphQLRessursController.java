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
import no.nav.nom.apps.api.graphql.ressurs.TelefonResolver;
import no.nav.nom.contract.nom.api.graphql.LederOrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursLeder;
import no.nav.nom.contract.nom.api.graphql.RessursOrgTilknytning;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.api.graphql.RessursWithMetadataResult;
import no.nav.nom.contract.nom.api.graphql.Telefon;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Controller
@PreAuthorize("@securityUtil.checkAuth(authentication)")
public class GraphQLRessursController {

    private final RessursResolver ressursResolver;
    private final RessursPropertyResolver propertyResolver;
    private final TelefonResolver telefonResolver;
    private final LederResolver lederResolver;
    private final OrgTilknytningResolver orgTilknytningResolver;
    private final GraphQLFieldConsumptionCounter counter;

    private final RessursStateRepository ressursStateRepository;

    @Autowired
    public GraphQLRessursController(
            RessursResolver ressursResolver,
            RessursPropertyResolver propertyResolver,
            TelefonResolver telefonResolver,
            LederResolver lederResolver,
            GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter,
            OrgTilknytningResolver orgTilknytningResolver,
            RessursStateRepository ressursStateRepository
    ) {
        this.ressursResolver = ressursResolver;
        this.propertyResolver = propertyResolver;
        this.telefonResolver = telefonResolver;
        this.lederResolver = lederResolver;
        this.counter = graphQLFieldConsumptionCounter;
        this.orgTilknytningResolver = orgTilknytningResolver;
        this.ressursStateRepository = ressursStateRepository;
    }

    @PreAuthorize(
            """
            @securityUtil.checkAuth(authentication) &&
            @securityUtil.hasPersonIdentAccess(authentication, #dataFetchingEnvironment.selectionSet)
            """
    )
    @QueryMapping("ressurs")
    public Optional<Ressurs> ressurs(@Argument("where") RessursSearch search, DataFetchingEnvironment dataFetchingEnvironment) {
        // Using @Argument("where") to tell Spring to bind the RessursSearch projected payload on the values below the "where" key.
        counter.count(dataFetchingEnvironment);
        return ressursResolver.ressurs(search);
    }

    @PreAuthorize(
            """
            @securityUtil.checkAuth(authentication) &&
            @securityUtil.hasPersonIdentAccess(authentication, #dataFetchingEnvironment.selectionSet)
            """
    )
    @QueryMapping("ressurser")
    public List<RessursResult> ressurser(@Argument("where") RessurserSearch search, DataFetchingEnvironment dataFetchingEnvironment) {
        counter.count(dataFetchingEnvironment);
        return ressursResolver.ressurser(search);
    }

    @PreAuthorize(
            """
            @securityUtil.checkAuth(authentication) &&
            @securityUtil.hasPersonIdentAccess(authentication, #dataFetchingEnvironment.selectionSet)
            """
    )
    @QueryMapping("ressurserWithMetadata")
    public List<RessursWithMetadataResult> ressurserWithMetadata(DataFetchingEnvironment dataFetchingEnvironment) {
        counter.count(dataFetchingEnvironment);
        return ressursResolver.ressurserWithMetadata();
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

    @BatchMapping(typeName = "Ressurs", field = "epost")
    public Mono<Map<Ressurs, DataFetcherResult<String>>> epost(List<Ressurs> ressurser) {
        return Mono.fromSupplier(() -> propertyResolver.epost(ressurser));
    }

    @BatchMapping(typeName = "Ressurs", field = "telefon")
    public Mono<Map<Ressurs, List<Telefon>>> telefon(List<Ressurs> ressurser) {
        return Mono.fromSupplier(() -> telefonResolver.load(ressurser));
    }


    private Map<Ressurs, String> getLookedupRessursPersonIdentMap(List<Ressurs> ressurser) {
        var ressursPersonIdentMap = new HashMap<Ressurs,String>(ressurser.size());
        for(Ressurs r : ressurser){
            var lookedUpPersonident = ressursStateRepository.findByNavident(r.getNavIdent(), RessursState::personident);
            ressursPersonIdentMap.put(r, lookedUpPersonident.orElse(null));
        }
        return ressursPersonIdentMap;
    }

}
