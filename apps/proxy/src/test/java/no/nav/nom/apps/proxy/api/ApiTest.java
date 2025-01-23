package no.nav.nom.apps.proxy.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static no.nav.nom.libs.test.SecurityTestUtils.getValidToken;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiTest extends ApiTestBase {
    public static final String SOME_JSON_BODY = "{\"some\": \"data\"}";
    public static final String SOME_UNIT4_ERROR_MESSAGE = "There was an error";

    @Value("${UNIT4_BASE_URL}")
    String unit4BaseUrl;

    @Value("${UNIT4_EMPLOYEES_PATH}")
    String unit4EmployeesPath;

    @Value("${UNIT4_API_PATH}")
    String unit4ApiPath;

    @Test
    @DisplayName("Tester at response koder og melding blir videreført")
    public void scenario41() {
        stubFor(post(appProperties.getUnit4().getEmployeesPath()).willReturn(aResponse().withStatus(HttpStatus.CREATED.value())
            .withHeader("Content-Type", "application/json")
            .withBody(SOME_JSON_BODY)));

        webTestClient
            .post()
            .uri("/proxy/unit4/ressurs")
            .body(Mono.just(SOME_JSON_BODY), String.class)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getValidToken(oAuth2Server)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isCreated();


        stubFor(post(appProperties.getUnit4().getEmployeesPath()).willReturn(aResponse().withStatus(HttpStatus.CONFLICT.value())
            .withHeader("Content-Type", "application/json")
            .withBody(SOME_JSON_BODY)));

        webTestClient
            .post()
            .uri("/proxy/unit4/ressurs")
            .body(Mono.just(SOME_JSON_BODY), String.class)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getValidToken(oAuth2Server)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT);


        stubFor(post(appProperties.getUnit4().getEmployeesPath()).willReturn(aResponse().withStatus(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .withHeader("Content-Type", "application/json")
            .withBody(SOME_JSON_BODY)));

        webTestClient
            .post()
            .uri("/proxy/unit4/ressurs")
            .body(Mono.just(SOME_JSON_BODY), String.class)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getValidToken(oAuth2Server)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("Unit4-endpoint returnerer 200 ved vellykket GET-request for ansatt")
    public void unit4_get_request() {
        stubFor(get(unit4EmployeesPath + "/123456")
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader("Content-Type", "application/json")
                .withBody(SOME_JSON_BODY)));

        webTestClient
            .get()
            .uri("/proxy/unit4/ressurs/123456")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getValidToken(oAuth2Server)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(SOME_JSON_BODY);
    }

    @Test
    @DisplayName("Unit4-endpoint returnerer riktig respons for GET-request ved feil fra Unit4-server")
    public void unit4_error_response() {
        stubFor(get(unit4EmployeesPath + "/123ASD")
            .willReturn(
                jsonResponse(
                    new Unit4ErrorResponse(1040, "Enheten for følgende parametre [Selskap: HR, ansatt: 123ASD] ble ikke funnet."),
                    HttpStatus.NOT_FOUND.value()
                ).withHeader("Content-Type", "application/json;charset=UTF-8")));

        webTestClient
            .get()
            .uri("/proxy/unit4/ressurs/123ASD")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getValidToken(oAuth2Server)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(String.class)
            .consumeWith(response ->
                assertThat(response.getResponseBody()).contains("Enheten for følgende parametre [Selskap: HR, ansatt: 123ASD] ble ikke funnet."));
    }

    @Test
    @DisplayName("Unit4-endpoint generisk GET-request")
    public void unit4_generic_get_request() {
        stubFor(get(unit4ApiPath + "/some/path")
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader("Content-Type", "application/json")
                .withBody(SOME_JSON_BODY)));

        webTestClient
            .get()
            .uri("/proxy/unit4/some/path")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getValidToken(oAuth2Server)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(SOME_JSON_BODY);
    }

    @Test
    @DisplayName("Unit4-endpoint kan sende POST-request")
    void unit4_successful_post() {
        stubFor(post(unit4EmployeesPath)
            .willReturn(created()));

        webTestClient
            .post()
            .uri("/proxy/unit4/ressurs")
            .body(Mono.just(SOME_JSON_BODY), String.class)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getValidToken(oAuth2Server)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isCreated();
    }

    @Test
    @DisplayName("Unit4-endpoint returnerer riktig respons for POST-request ved feil fra Unit4-server")
    void unit4_correct_response_post_error() {
        stubFor(post(unit4EmployeesPath)
            .willReturn(
                jsonResponse(
                    new Unit4ErrorResponse(9999, SOME_UNIT4_ERROR_MESSAGE),
                    HttpStatus.BAD_REQUEST.value()
                ).withHeader("Content-Type", "application/json;charset=UTF-8")));

        webTestClient
            .post()
            .uri("/proxy/unit4/ressurs")
            .body(Mono.just(SOME_JSON_BODY), String.class)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getValidToken(oAuth2Server)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(String.class)
            .consumeWith(response ->
                assertThat(response.getResponseBody()).contains(SOME_UNIT4_ERROR_MESSAGE));
    }

    @Test
    @DisplayName("Unit4-endpoint kan sende PATCH-request")
    void unit4_successful_patch() {
        stubFor(patch(urlMatching(unit4EmployeesPath + "/123456"))
            .willReturn(ok()));

        webTestClient
            .patch()
            .uri("/proxy/unit4/ressurs/123456")
            .body(Mono.just(SOME_JSON_BODY), String.class)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getValidToken(oAuth2Server)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("Unit4-endpoint kan sende DELETE-request")
    void unit4_successful_delete() {
        stubFor(delete(unit4EmployeesPath + "/123456")
            .willReturn(ok()));

        webTestClient
            .delete()
            .uri("/proxy/unit4/ressurs/123456")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getValidToken(oAuth2Server)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk();
    }

    private record Unit4ErrorResponse(
        int code,
        String message
    ) {}

}
