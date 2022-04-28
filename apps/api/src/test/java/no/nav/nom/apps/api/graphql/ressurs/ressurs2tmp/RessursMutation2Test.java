package no.nav.nom.apps.api.graphql.ressurs.ressurs2tmp;

import lombok.val;
import no.nav.nom.apps.api.graphql.ressurs.RessursTestBase;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RessursMutation2Test extends RessursTestBase {

    @Autowired
    IRessursService iRessursService;

    @Test
    @DisplayName("Opprett ressurs2")
    void scenario12() {
        val personIdent = "12345678901____X";
        pdlUtils.mockSinglePersonResponse(personIdent);

        var response = postForResource("ressurs/opprettRessurs2.graphql", false, Map.of("request", Map.of(
                "personident", personIdent, "fornavn", "Vidar", "etternavn", "Vislesen"
        )));
        assertThat(response.get("$.data.opprettRessurs2.navident")).isEqualTo("Z999999");
        assertThat(response.get("$.data.opprettRessurs2.personident")).isEqualTo(personIdent);

        assertThat(response.get("$.data.opprettRessurs2.person.navn.fornavn")).isEqualTo("fornavn");
        assertThat(response.get("$.data.opprettRessurs2.person.navn.mellomnavn")).isEqualTo("mellomnavn");
        assertThat(response.get("$.data.opprettRessurs2.person.navn.etternavn")).isEqualTo("etternavn");

        var x = iRessursService.findByNavIdent("Z999999");
        assertThat(x).isNotEmpty();
    }

    @Test
    @DisplayName("Opprett ressurs2, gjentagende requests håndteres OK")
    void scenario13() {
        val personIdent = "12345678901____X";
        val personIdent2 = "12345678901____Y";
        pdlUtils.mockSinglePersonResponse(personIdent);
        pdlUtils.mockSinglePersonResponse(personIdent2);

        var response = postForResource("ressurs/opprettRessurs2.graphql", false, Map.of("request", Map.of(
                "personident", personIdent, "fornavn", "Vidar", "etternavn", "Vislesen"
        )));

        var response2 = postForResource("ressurs/opprettRessurs2.graphql", false, Map.of("request", Map.of(
                "personident", personIdent, "fornavn", "VidarEkstra", "etternavn", "VislesenEkstra"
        )));

        var response3 = postForResource("ressurs/opprettRessurs2.graphql", false, Map.of("request", Map.of(
                "personident", personIdent2, "fornavn", "Vidar2", "etternavn", "Vislesen2"
        )));

        assertThat(response3.get("$.data.opprettRessurs2.navident")).isEqualTo("Z999999");
        assertThat(response3.get("$.data.opprettRessurs2.personident")).isEqualTo(personIdent2);

        assertThat(response3.get("$.data.opprettRessurs2.person.navn.fornavn")).isEqualTo("fornavn");
        assertThat(response3.get("$.data.opprettRessurs2.person.navn.mellomnavn")).isEqualTo("mellomnavn");
        assertThat(response3.get("$.data.opprettRessurs2.person.navn.etternavn")).isEqualTo("etternavn");

        var x = iRessursService.findByNavIdent("Z999999");
        assertThat(x).isNotEmpty();
        assertThat(x.get().getPersonIdent()).isEqualTo(personIdent2);
    }


}
