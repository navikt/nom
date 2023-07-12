package no.nav.nom.apps.nom.unit4.api;

import com.nimbusds.jose.JOSEException;
import com.thomaskasene.wiremock.junit.WireMockStubs;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.unit4.RsaStub;
import no.nav.nom.apps.nom.unit4.SecurityTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;


@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@WireMockStubs(RsaStub.class)
@EmbeddedKafka(partitions = 1, topics = {"nom-ressurser"}, controlledShutdown = true)
@ExtendWith(OutputCaptureExtension.class)
@Import(SecurityTestUtils.class)
@Slf4j
class SecurityTest {

    private static final String ADMIN_API_PATH = "/api/v1/admin/ressurs/";
    public static final String RESSURS_API_PATH = "/api/ressurs";

    public static final String SWAGGER_REDIRECT_API_PATH = "/api";
    public static final String ACTUATOR_ENDPOINT_PATH = "/internal/health/liveness";
    public static final String ID_MOCKED_USER = "123456";


    @Autowired
    SecurityTestUtils securityTestUtils;

    @Autowired
    WebTestClient webTestClient;


    @Test
    @DisplayName("Gyldig token gir 200 OK respons")
    @SneakyThrows
    public void should_get_admin_ressurs_with_valid_token() {
        var token = securityTestUtils.createToken(securityTestUtils.validJwtClaims());

        webTestClient.get()
            .uri(ADMIN_API_PATH + ID_MOCKED_USER)
            //.accept(MediaType.APPLICATION_JSON)
            .headers(httpHeaders -> {
                httpHeaders.setBearerAuth(token);
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
        var token = securityTestUtils.createToken(
            securityTestUtils.validJwtClaims("testSubject", "someRandomAudience"));

        webTestClient.get()
            .uri(ADMIN_API_PATH + "123456")
            .headers(httpHeaders -> {
                httpHeaders.setBearerAuth(token);
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

/*    @Test
    @DisplayName("Kan nå Swagger API uten token")
    void should_access_swagger_api_without_token() {
        webTestClient.get()
            .uri(SWAGGER_REDIRECT_API_PATH)
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            })
            .exchange()
            .expectStatus().isOk();
    }*/

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
