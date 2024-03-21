package no.nav.nom.apps.api.rest.person;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.rest.RestTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class PersonTest extends RestTestBase {

    @Autowired
    TestTokensGenerator testToken;

    @Test
    @SneakyThrows
    @DisplayName("Kan hente fulle navn fra PDL for person som er ressurs")
    void scenario1() {
        String personident = "12345678910";
        val expectedNavn = pdlUtils.mockSinglePersonResponse();

        webTestClient.post()
            .uri("/api/v1/person/getPerson")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(testToken.queryPersonClient());
            })
            .bodyValue(new PdlPersonRequest(personident))
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(PdlPersonDTO.class)
            .isEqualTo(PdlPersonDTO.builder().navn(expectedNavn)
                .build()
            );
    }


    @Test
    @SneakyThrows
    @DisplayName("Ingen tilgang uten QUERY_PERSON authority")
    void scenario2() {
        var path = "/api/v1/person/getPerson";

        var responseBody = webTestClient.post()
            .uri(path)
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(testToken.noScopesClient());
            })
            .bodyValue(new PdlPersonRequest("12345678910"))
            .exchange()
            .expectStatus().isForbidden()
            .expectBody(JsonNode.class)
            .returnResult().getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(responseBody.size(), 4);
        Assertions.assertEquals(responseBody.get("error").asText(), "Forbidden");
        Assertions.assertEquals(responseBody.get("path").asText(), path);
        Assertions.assertTrue(responseBody.has("status"));
        Assertions.assertTrue(responseBody.has("timestamp"));

    }
}
