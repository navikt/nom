package no.nav.nom.apps.proxy.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.net.URI;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ApiTest extends ApiTestBase {
    private static final String DVH_URL = "/proxy/enheter";

    @Test
    @DisplayName("Tester dvh kall")
    public void scenario21(){
        val claims = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(claims);

        val headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create(DVH_URL), HttpMethod.GET, new HttpEntity<>(null, headers), JsonNode.class);
        assertThat(result.getStatusCode(), is(HttpStatus.OK));

    }

    @Test
    @DisplayName("Tester tel kall")
    public void scenario31(){
        val claims = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(claims);

        val headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create("/proxy/tel"), HttpMethod.GET, new HttpEntity<>(null, headers), JsonNode.class);
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
    }

}
