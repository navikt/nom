package no.nav.nom.apps.api.graphql.controller;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.common.projections.PersonSearch;
import no.nav.nom.apps.api.graphql.common.resolver.FolkeregisterPersonResolver;
import no.nav.nom.apps.api.graphql.person.PersonResolver;
import no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson;
import no.nav.nom.contract.nom.api.graphql.PersonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Controller // Oppslag PDL - Feier uansett - kan fjernes
@PreAuthorize("@securityUtil.checkAuth(authentication)")
public class GraphQLPersonController {

    private final PersonResolver personResolver;
    private final FolkeregisterPersonResolver folkeregisterPersonResolver;
    private final GraphQLFieldConsumptionCounter counter;

    @Autowired
    public GraphQLPersonController(
            PersonResolver personResolver,
            FolkeregisterPersonResolver folkeregisterPersonResolver,
            GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter
    ) {
        this.personResolver = personResolver;
        this.folkeregisterPersonResolver = folkeregisterPersonResolver;
        this.counter = graphQLFieldConsumptionCounter;
    }

    @QueryMapping("person")  // Oppslag PDL - Feier uansett - kan fjernes
    public Optional<PersonResult> person(@Argument("where") PersonSearch personSearch, DataFetchingEnvironment environment) {
        counter.count(environment);
        return personSearch.resolveIdent().flatMap(personResolver::person);
    }

    @Deprecated
    @SchemaMapping(typeName = "PersonResult", field = "person")
    public Mono<DataFetcherResult<FolkeregisterPerson>> person(PersonResult person) {
        return folkeregisterPerson(person);
    }

    @SchemaMapping(typeName = "PersonResult", field = "folkeregisterPerson")
    public Mono<DataFetcherResult<FolkeregisterPerson>> folkeregisterPerson(PersonResult person) {
        return folkeregisterPersonResolver.folkeregisterPerson(person, PersonResult::getPersonIdent);
    }
}
