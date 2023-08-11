package no.nav.nom.apps.proxy.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JOSEException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ApiTest extends ApiTestBase {
    private static final String DVH_ENHETER_URL = "/proxy/dvh/json/Hr/NOM2/NOM_HRORG_HIERARKI_v1";
    public static final String SOME_JSON_BODY = "{\"some\": \"data\"}";

    @Value("${UNIT4_BASE_URL}")
    String unit4BaseUrl;

    @Value("${UNIT4_EMPLOYEES_PATH}")
    String unit4EmployeesPath;

    @Test
    @DisplayName("Tester dvh kall")
    public void scenario21() throws Exception {
        val claims = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(claims);

        val headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create(DVH_ENHETER_URL), HttpMethod.GET, new HttpEntity<>(null, headers), JsonNode.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    @Test
    @DisplayName("Tester tel kall")
    public void scenario31() throws Exception {
        val claims = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(claims);

        val headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create("/proxy/tel"), HttpMethod.GET, new HttpEntity<>(null, headers), JsonNode.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Tester generelt DVH proxy kall")
    public void scenario32() throws Exception {
        val claims = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(claims);

        val headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create("/proxy/dvh/get-proxy-test-mapping"), HttpMethod.GET, new HttpEntity<>(null, headers), JsonNode.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Tester generelt DVH proxy kall med query parametre")
    public void scenario33() throws Exception {
        val claims = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(claims);

        val headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(
            URI.create("/proxy/dvh/get-proxy-test-mapping?testParamOne=1&testParamTwo=2"),
            HttpMethod.GET,
            new HttpEntity<>(null, headers),
            JsonNode.class
        );
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Tester at response koder og melding blir videreført")
    public void scenario41() throws Exception {
        val claims = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(claims);

        val headers = new HttpHeaders();
        headers.setBearerAuth(token);


        stubFor(post(appProperties.getUnit4().getEmployeesPath()).willReturn(aResponse().withStatus(HttpStatus.CREATED.value())
            .withHeader("Content-Type", "application/json")
            .withBody(SOME_JSON_BODY)));

        val result = restTemplate.exchange(URI.create("/proxy/unit4/ressurs"), HttpMethod.POST, new HttpEntity<>("Boddyyy", headers), String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);


        stubFor(post(appProperties.getUnit4().getEmployeesPath()).willReturn(aResponse().withStatus(HttpStatus.CONFLICT.value())
            .withHeader("Content-Type", "application/json")
            .withBody(SOME_JSON_BODY)));

        val result2 = restTemplate.exchange(URI.create("/proxy/unit4/ressurs"), HttpMethod.POST, new HttpEntity<>("Boddyyy", headers), String.class);
        assertThat(result2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        stubFor(post(appProperties.getUnit4().getEmployeesPath()).willReturn(aResponse().withStatus(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .withHeader("Content-Type", "application/json")
            .withBody(SOME_JSON_BODY)));

        val result3 = restTemplate.exchange(URI.create("/proxy/unit4/ressurs"), HttpMethod.POST, new HttpEntity<>("Boddyyy", headers), String.class);
        assertThat(result3.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("Unit4-endpoint gir 200 respons ved vellykket kall")
    public void unit4_get_request() {
        stubFor(get(unit4EmployeesPath + "/123456")
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader("Content-Type", "application/json")
                .withBody(SOME_JSON_BODY)));

        webTestClient.get()
            .uri("/proxy/unit4/ressurs/123456")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getValidToken()))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(SOME_JSON_BODY);
    }

    @Test
    @DisplayName("Unit4-endpoint returnerer riktig respons ved feil fra Unit4")
    public void unit4_error_response() {
        stubFor(get(unit4EmployeesPath + "/123ASD")
            .willReturn(
                jsonResponse(
                    new Unit4ErrorResponse(1040, "Enheten for følgende parametre [Selskap: HR, ansatt: 123ASD] ble ikke funnet."),
                    HttpStatus.NOT_FOUND.value()
                ).withHeader("Content-Type", "application/json;charset=UTF-8")));

        webTestClient.get()
            .uri("/proxy/unit4/ressurs/123ASD")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getValidToken()))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(String.class)
            .consumeWith(response ->
                assertThat(response.getResponseBody()).contains("Enheten for følgende parametre [Selskap: HR, ansatt: 123ASD] ble ikke funnet."));
    }

    private record Unit4ErrorResponse(
        int code,
        String message
    ) {}

    private String getValidToken() {
        val claims = securityTestUtils.validJwtClaims();
        try {
            return securityTestUtils.createToken(claims);
        } catch (JOSEException e) {
            throw new RuntimeException("Unable to create token: " + e.getMessage(), e);
        }
    }
}
