package no.nav.nom.apps.api.graphql.controller;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.common.projections.PersonSearch;
import no.nav.nom.apps.api.graphql.common.resolver.FolkeregisterPersonResolver;
import no.nav.nom.apps.api.graphql.person.PersonResolver;
import no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson;
import no.nav.nom.contract.nom.api.graphql.PersonResult;
import no.nav.nom.contract.nom.api.graphql.RessursNavn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class GraphQLPersonController {

    private final PersonResolver personResolver;
    private final FolkeregisterPersonResolver folkeregisterPersonResolver;
    private final GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter;

    @Autowired
    public GraphQLPersonController(
            PersonResolver personResolver,
            FolkeregisterPersonResolver folkeregisterPersonResolver,
            GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter
    ) {
        this.personResolver = personResolver;
        this.folkeregisterPersonResolver = folkeregisterPersonResolver;
        this.graphQLFieldConsumptionCounter = graphQLFieldConsumptionCounter;
    }

    @QueryMapping("person")
    public Optional<PersonResult> person(@Argument("where") PersonSearch personSearch, DataFetchingEnvironment environment) {
        // TODO: Add security check for preauth?
        graphQLFieldConsumptionCounter.count(environment);
        return personSearch.resolveIdent().flatMap(personResolver::person);
    }

    @Deprecated
    @SchemaMapping(typeName = "PersonResult", field = "person")
    public DataFetcherResult<FolkeregisterPerson> person(PersonResult person) {
        return folkeregisterPerson(person);
    }

    @SchemaMapping(typeName = "PersonResult", field = "folkeregisterPerson")
    public DataFetcherResult<FolkeregisterPerson> folkeregisterPerson(PersonResult person) {
        return folkeregisterPersonResolver.folkeregisterPerson(person, PersonResult::getPersonIdent);
    }
}
