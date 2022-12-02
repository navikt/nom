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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
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

    @Test
    @DisplayName("Tester generelt proxy kall")
    public void scenario32(){
        val claims = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(claims);

        val headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create("/proxy/dvh/get-proxy-test-mapping"), HttpMethod.GET, new HttpEntity<>(null, headers), JsonNode.class);
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    @DisplayName("Tester generelt proxy kall med query parametre")
    public void scenario33(){
        val claims = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(claims);

        val headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create("/proxy/dvh/get-proxy-test-mapping?testParamOne=1&testParamTwo=2"), HttpMethod.GET, new HttpEntity<>(null, headers), JsonNode.class);
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    @DisplayName("Tester at response koder og melding blir videreført")
    public void scenario41(){
        val claims = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(claims);

        val headers = new HttpHeaders();
        headers.setBearerAuth(token);


        stubFor(post(Unit4Controller.API_EMPLOYEES_PATH).willReturn(aResponse().withStatus(HttpStatus.CREATED.value()).withHeader("Content-Type", "application/json").withBody("response")));

        val result = restTemplate.exchange(URI.create("/proxy/unit4/ressurs"), HttpMethod.POST, new HttpEntity<>("Boddyyy", headers), String.class);
        assertThat(result.getStatusCode(), is(HttpStatus.CREATED));


        stubFor(post(Unit4Controller.API_EMPLOYEES_PATH).willReturn(aResponse().withStatus(HttpStatus.CONFLICT.value()).withHeader("Content-Type", "application/json").withBody("response")));

        val result2 = restTemplate.exchange(URI.create("/proxy/unit4/ressurs"), HttpMethod.POST, new HttpEntity<>("Boddyyy", headers), String.class);
        assertThat(result2.getStatusCode(), is(HttpStatus.CONFLICT));

        stubFor(post(Unit4Controller.API_EMPLOYEES_PATH).willReturn(aResponse().withStatus(HttpStatus.UNPROCESSABLE_ENTITY.value()).withHeader("Content-Type", "application/json").withBody("response")));

        val result3 = restTemplate.exchange(URI.create("/proxy/unit4/ressurs"), HttpMethod.POST, new HttpEntity<>("Boddyyy", headers), String.class);
        assertThat(result3.getStatusCode(), is(HttpStatus.UNPROCESSABLE_ENTITY));
    }

}
