package no.nav.nom.apps.api.graphql.ressurs;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("GraphQL.OpprettRessurs")
public class RessursMutationTest extends RessursTestBase {


    //@Test
    @DisplayName("Opprett ressurs")
    void scenario11() {
        val personIdent = "12345678901";
        pdlUtils.mockSinglePersonResponse(personIdent);

        var response = postForResource("ressurs/opprettRessurs.graphql", false, Map.of("personIdent", personIdent));
        //assertThat(response.get("$.data.opprettRessurs.nomId")).isNotNull();
        assertThat(response.get("$.data.opprettRessurs.navIdent")).isEqualTo("E000000");
        assertThat(response.get("$.data.opprettRessurs.personIdent")).isEqualTo(personIdent);

        assertThat(response.get("$.data.opprettRessurs.person.navn.fornavn")).isEqualTo("fornavn");
        assertThat(response.get("$.data.opprettRessurs.person.navn.mellomnavn")).isEqualTo("mellomnavn");
        assertThat(response.get("$.data.opprettRessurs.person.navn.etternavn")).isEqualTo("etternavn");

    }
}
