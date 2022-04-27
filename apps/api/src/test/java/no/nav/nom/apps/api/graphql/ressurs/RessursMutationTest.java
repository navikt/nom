package no.nav.nom.apps.api.graphql.ressurs;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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


    @Test
    @DisplayName("Opprett ressurs2")
    void scenario12() {
        val personIdent = "12345678901____X";
        pdlUtils.mockSinglePersonResponse(personIdent);

        var response = postForResource("ressurs/opprettRessurs2.graphql", false, Map.of("request", Map.of(
                "personident", personIdent,  "fornavn", "Vidar", "etternavn","Vislesen"
        )));
        //assertThat(response.get("$.data.opprettRessurs.nomId")).isNotNull();
        assertThat(response.get("$.data.opprettRessurs2.navident")).isEqualTo("Z999999");
        assertThat(response.get("$.data.opprettRessurs2.personident")).isEqualTo(personIdent);

        assertThat(response.get("$.data.opprettRessurs2.person.navn.fornavn")).isEqualTo("fornavn");
        assertThat(response.get("$.data.opprettRessurs2.person.navn.mellomnavn")).isEqualTo("mellomnavn");
        assertThat(response.get("$.data.opprettRessurs2.person.navn.etternavn")).isEqualTo("etternavn");

    }
}
