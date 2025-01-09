package no.nav.nom.apps.nom.unit4.api;

import com.nimbusds.jose.JOSEException;
import lombok.SneakyThrows;
import no.nav.nom.apps.nom.unit4.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

import static no.nav.nom.libs.test.SecurityTestUtils.getTokenWithInvalidAudience;
import static no.nav.nom.libs.test.SecurityTestUtils.getValidToken;


@ExtendWith(OutputCaptureExtension.class)
class SecurityTest extends TestBase {

    private static final String ADMIN_API_PATH = "/api/v1/admin/ressurs/";
    public static final String RESSURS_API_PATH = "/api/ressurs";
    public static final String SWAGGER_API_PATH = "/api";
    public static final String ACTUATOR_ENDPOINT_PATH = "/internal/health/liveness";
    public static final String ID_MOCKED_USER = "123456";
    

    @Test
    @DisplayName("Gyldig token gir 200 OK respons")
    @SneakyThrows
    public void should_get_admin_ressurs_with_valid_token() {
        webTestClient.get()
            .uri(ADMIN_API_PATH + ID_MOCKED_USER)
            //.accept(MediaType.APPLICATION_JSON)
            .headers(httpHeaders -> {
                httpHeaders.setBearerAuth(getValidToken(oAuth2Server));
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            })
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("Uten token gir respons 401 Unauthorized")
    void should_get_401_unauthorized_without_token() {
        webTestClient.get()
            .uri(ADMIN_API_PATH + ID_MOCKED_USER)
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            })
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Ugyldig token gir respons 401 Unauthorized")
    void should_get_401_unauthorized_with_invalid_token() {
        webTestClient.get()
            .uri(ADMIN_API_PATH + ID_MOCKED_USER)
            .headers(httpHeaders -> {
                httpHeaders.setBearerAuth("not a real token");
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            })
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Feil audience gir respons 401 Unauthorized")
    void should_get_401_unauthorized_with_wrong_audience() throws JOSEException {
        webTestClient.get()
            .uri(ADMIN_API_PATH + "123456")
            .headers(httpHeaders -> {
                httpHeaders.setBearerAuth(getTokenWithInvalidAudience(oAuth2Server));
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            })
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Manglende token gir respons 401 Unauthorized fra ressurs API")
    void should_get_401_unauthorized_without_token_on_ressurs_api() {
        webTestClient.post()
            .uri(RESSURS_API_PATH)
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            })
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Kan nå Swagger API uten token")
    void should_access_swagger_api_without_token() {
        webTestClient.get()
            .uri(SWAGGER_API_PATH)
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            })
            .exchange()
            .expectStatus().is3xxRedirection();
    }

    @Test
    @DisplayName("Kan nå actuator endpoint uten token")
    void should_access_actuator_without_token() {
        webTestClient.get()
            .uri(ACTUATOR_ENDPOINT_PATH)
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            })
            .exchange()
            .expectStatus().isOk();
    }

}
