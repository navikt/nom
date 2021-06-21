package no.nav.nom.apps.api.graphql.person;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.graphql.GraphQLTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GraphQL.SøkPerson")
public class PersonTest extends GraphQLTestBase {

    @Test
    @SneakyThrows
    @DisplayName("Kan hente fulle navn fra PDL for person som er ressurs")
    void scenario1() {
        kafkaUtils.sendHendelseFromKafkaMock(createRessursHendelse("3"));
        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent3");

        val response = postForResource("person/scenario1.graphql", false, Map.of("fnr", "testPersonIdent3"));

        val person = response.get("$.data.person", JsonNode.class);
        assertThat(person.get("personIdent").textValue()).isEqualTo("testPersonIdent3");
        assertThat(person.get("nomId").textValue()).isNotNull();
        assertThat(person.get("navIdent").textValue()).isNotNull();

        pdlUtils.assertNavn(person.get("personIdent").textValue(), person.get("person").get("navn"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Kan hente fulle navn fra PDL for person som ikke er ressurs")
    void scenario2() {
        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent3");

        val response = postForResource("person/scenario1.graphql", false, Map.of("fnr", "testPersonIdent3"));

        val person = response.get("$.data.person", JsonNode.class);
        assertThat(person.get("personIdent").textValue()).isEqualTo("testPersonIdent3");
        assertThat(person.get("nomId").textValue()).isNull();
        assertThat(person.get("navIdent").textValue()).isNull();

        pdlUtils.assertNavn(person.get("personIdent").textValue(), person.get("person").get("navn"));
    }

}
