package no.nav.nom.apps.api.graphql.ressurs;

import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("GraphQL.OpprettRessurs")
public class RessursMutationTest extends RessursTestBase {


    //@Test
    @DisplayName("Opprett ressurs")
    void scenario11() {
        final var personIdent = "12345678901";
        pdlUtils.mockSinglePersonResponse(personIdent);

        var response = postForResource("ressurs/opprettRessurs.graphql", Map.of("personIdent", personIdent));
        //assertThat(response.path("opprettRessurs.nomId").entity(String.class).get()).isNotNull();
        assertThat(response.path("opprettRessurs.navIdent").entity(String.class).get()).isEqualTo("E000000");
        assertThat(response.path("opprettRessurs.personIdent").entity(String.class).get()).isEqualTo(personIdent);

        assertThat(response.path("opprettRessurs.person.navn.fornavn").entity(String.class).get()).isEqualTo("fornavn");
        assertThat(response.path("opprettRessurs.person.navn.mellomnavn").entity(String.class).get()).isEqualTo("mellomnavn");
        assertThat(response.path("opprettRessurs.person.navn.etternavn").entity(String.class).get()).isEqualTo("etternavn");

    }
}
