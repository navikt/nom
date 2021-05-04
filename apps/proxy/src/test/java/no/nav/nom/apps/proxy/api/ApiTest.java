package no.nav.nom.apps.proxy.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ApiTest extends ApiTestBase {
    private static final String PDL_URL = "/proxy/pdl";

    @Test
    @DisplayName("Tester at pdl proxy ruter kallet videre riktig")
    public void scenario11(){
        val bodyRequest = UUID.randomUUID().toString();
        val bodyResponse = UUID.randomUUID().toString();
        val tema = "temaa";
        val claims = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(claims);

        claims.audience("dvh").subject("proxy");
        val tokenSts = securityTestUtils.createToken(claims);


        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.add("Tema", tema);
        stubFor(get(WireMock.urlMatching("/rest/v1/sts/token.*"))
                .willReturn(aResponse().withBody(String.format("{\"access_token\": \"[%s]\"}", tokenSts)).withHeader("Content-Type", "application/json"))
        );

        stubFor(post("/graphql")
                .withHeader("Tema", equalTo(tema))
                .withHeader("Nav-Consumer-Token", containing(tokenSts))
                .withHeader("Authorization", containing(tokenSts))
                .withRequestBody(equalTo(bodyRequest))
                .willReturn(aResponse().withBody("{\"result\":\""+bodyResponse+"\"}").withHeader("Content-Type", "application/json"))
        );

        val result = restTemplate.exchange(URI.create(PDL_URL), HttpMethod.POST, new HttpEntity<>(bodyRequest, headers), JsonNode.class);
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
        Assertions.assertEquals(result.getBody().get("result").asText(), bodyResponse);
    }
}
