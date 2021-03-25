package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DisplayName("GraphQL.HentRessurs")
class RessursTest extends RessursTestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Test
    @SneakyThrows
    @DisplayName("Sender inn ressurs fra kafka og leser inn med graphql navIdent parameter")
    void scenario11() {
        val inputRessurs = ressursTestTemplate("");
        kafkaUtils.sendFromKafkaMock(inputRessurs);

        val response = postForResource("ressurs/scenario1.graphql", false);

        val outputRessurs = response.getList("$.data.ressurs", Ressurs.class).get(0);
        assertEquals(inputRessurs.getNavident(), outputRessurs.getNavIdent());
        assertEquals(inputRessurs.getPersonIdent(), outputRessurs.getPersonIdent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Sender inn ressurs fra kafka og leser inn med graphql personIdent parameter")
    void scenario12() {
        val inputRessurs = ressursTestTemplate("");
        kafkaUtils.sendFromKafkaMock(inputRessurs);

        val response = postForResource("ressurs/scenario2.graphql", false);

        val outputRessurs = response.getList("$.data.ressurs", Ressurs.class).get(0);
        assertEquals(inputRessurs.getNavident(), outputRessurs.getNavIdent());
        assertEquals(inputRessurs.getPersonIdent(), outputRessurs.getPersonIdent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Sender inn ressurs fra kafka og leser inn med graphql uten parameter")
    void scenario13() {
        val inputRessurs1 = ressursTestTemplate("1");
        val inputRessurs2 = ressursTestTemplate("2");
        val inputRessurs3 = ressursTestTemplate("3");

        kafkaUtils.sendFromKafkaMock(inputRessurs1, inputRessurs2, inputRessurs3);

        val response = postForResource("ressurs/scenario3.graphql", false);

        val outputRessurs = response.getList("$.data.ressurs", Ressurs.class);
        assertEquals(3, outputRessurs.size());
    }

    @Test
    @SneakyThrows
    @DisplayName("Kan hente ut kobling for ressurs")
    void scenario21() {
        val inputRessurs = ressursTestTemplate("2");

        kafkaUtils.sendFromKafkaMock(inputRessurs);
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("ressurs/scenario42.graphql", false);

        val ressurs = response.getList("$.data.ressurs", JsonNode.class).get(0);
        val actualOrganisasjonsEnhetId = ressurs.get("koblinger").get(0).get("organisasjonsenhet").get("agressoId").asText();
        assertEquals("testId2", actualOrganisasjonsEnhetId);
    }

    @Test
    @SneakyThrows
    @DisplayName("Kobling er tom liste om kobling for person ikke finnes")
    void scenario22() {
        val inputRessurs = ressursTestTemplate("2");
        kafkaUtils.sendFromKafkaMock(inputRessurs);

        val response = postForResource("ressurs/scenario42.graphql", false);

        val ressurs = response.getList("$.data.ressurs", JsonNode.class).get(0);
        assertTrue(ressurs.get("koblinger").isEmpty());
    }

    @Test
    @SneakyThrows
    @DisplayName("Kan hente fulle navn fra PDL")
    void scenario31() {
        val inputRessurs = ressursTestTemplate("3");
        val inputRessurs2 = ressursTestTemplate("4");
        kafkaUtils.sendFromKafkaMock(inputRessurs, inputRessurs2);
        pdlUtils.addPersonToPdlResponse(true, inputRessurs.getPersonIdent(), inputRessurs2.getPersonIdent());

        val response = postForResource("ressurs/scenario5.graphql", false);

        val ressurser = response.getList("$.data.ressurs", JsonNode.class);
        assertEquals(2, ressurser.size());

        pdlUtils.assertNavn(ressurser.get(0).get("personIdent").asText(), ressurser.get(0).get("person").get("navn"));
        pdlUtils.assertNavn(ressurser.get(1).get("personIdent").asText(), ressurser.get(1).get("person").get("navn"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Kan hente navn uten mellomnavn fra PDL")
    void scenario32() {
        val inputRessurs = ressursTestTemplate("3");
        val inputRessurs2 = ressursTestTemplate("4");
        kafkaUtils.sendFromKafkaMock(inputRessurs, inputRessurs2);
        pdlUtils.addPersonToPdlResponse(false, inputRessurs.getPersonIdent(), inputRessurs2.getPersonIdent());

        val response = postForResource("ressurs/scenario5.graphql", false);

        val ressurser = response.getList("$.data.ressurs", JsonNode.class);
        assertEquals(2, ressurser.size());

        pdlUtils.assertNavn(ressurser.get(0).get("personIdent").asText(), ressurser.get(0).get("person").get("navn"));
        pdlUtils.assertNavn(ressurser.get(1).get("personIdent").asText(), ressurser.get(1).get("person").get("navn"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Hente navn hvor PDL er utilgjengelig")
    void scenario33() {
        val inputRessurs = ressursTestTemplate("3");
        val inputRessurs2 = ressursTestTemplate("4");
        kafkaUtils.sendFromKafkaMock(inputRessurs, inputRessurs2);
        pdlUtils.addPersonToPdlResponse(false, inputRessurs.getPersonIdent(), inputRessurs2.getPersonIdent());
        stubFor(post("/graphql").willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));


        val response = postForResource("ressurs/scenario5.graphql", true);

        val errors = response.getList("$.errors", JsonNode.class);
        assertEquals(2, errors.size());

        val ressurs = response.getList("$.data.ressurs", JsonNode.class).get(0);
        assertTrue(ressurs.hasNonNull("navIdent"));
        assertTrue(ressurs.hasNonNull("personIdent"));
        assertFalse(ressurs.hasNonNull("person"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Hente navn hvor PDL er gir feil og manglende respons")
    void scenario34() {
        val inputRessurs = ressursTestTemplate("3");
        val inputRessurs2 = ressursTestTemplate("4");

        kafkaUtils.sendFromKafkaMock(inputRessurs, inputRessurs2);
        pdlUtils.addErrorToPdlResponse(inputRessurs.getPersonIdent(), "bad_request");
        pdlUtils.addErrorToPdlResponse(inputRessurs2.getPersonIdent(), "not_found");

        val response = postForResource("ressurs/scenario5.graphql", true);

        val errors = response.getList("$.errors", JsonNode.class);
        assertEquals(2, errors.size());

        val ressurs = response.getList("$.data.ressurs", JsonNode.class).get(0);
        assertTrue(ressurs.hasNonNull("navIdent"));
        assertTrue(ressurs.hasNonNull("personIdent"));
        assertFalse(ressurs.hasNonNull("person"));
    }
}
