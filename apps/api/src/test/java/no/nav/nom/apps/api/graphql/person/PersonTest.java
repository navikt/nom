package no.nav.nom.apps.api.graphql.person;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import no.nav.nom.apps.api.graphql.GraphQLTestBase;
import no.nav.nom.apps.api.ressurs.contract.IRessurs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GraphQL.SøkPerson")
public class PersonTest extends GraphQLTestBase {

    @Test
    @SneakyThrows
    @DisplayName("Kan hente fulle navn fra PDL for person som er ressurs")
    void scenario1() {
        IRessurs ressurs = createRessursHendelse("3");
        pdlUtils.addPersonToPdlResponse(true, ressurs.getPersonIdent());

        final var response = postForResource("person/scenario1.graphql", singletonMap("fnr", "testPersonIdent3"));

        response.path("person").entity(JsonNode.class)
                .satisfies(person -> assertThat(person.get("personIdent").textValue()).isEqualTo("testPersonIdent3"))
                //.satisfies(person -> assertThat(person.get("nomId").textValue()).isNotNull())
                .satisfies(person -> assertThat(person.get("navIdent").textValue()).isNotNull())
                .satisfies(person -> pdlUtils.assertNavn(person.get("personIdent").textValue(), person.get("person").get("navn")));
    }

    @Test
    @SneakyThrows
    @DisplayName("Kan hente fulle navn fra PDL for person som ikke er ressurs")
    void scenario2() {
        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent3");

        final var response = postForResource("person/scenario1.graphql", singletonMap("fnr", "testPersonIdent3"));

        response.path("person").entity(JsonNode.class)
                .satisfies(person -> assertThat(person.get("personIdent").textValue()).isEqualTo("testPersonIdent3"))
                //.satisfies(person -> assertThat(person.get("nomId").textValue()).isNull())
                .satisfies(person -> assertThat(person.get("navIdent").textValue()).isNull())
                .satisfies(person -> pdlUtils.assertNavn(person.get("personIdent").textValue(), person.get("person").get("navn")));
    }

}
