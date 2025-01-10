package no.nav.nom.apps.proxy.api;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static no.nav.nom.libs.test.SecurityTestUtils.*;


public class SecurityTest extends ApiTestBase {

    private static final String stillingerURL = "/proxy/dvh/json/Hr/NOM2/stilling2nom";
    private static final String ressurserURL = "/proxy/dvh/json/Hr/NOM2/NOM_ANSATTE_BUCKETS_v1";
    private static final String enheterURL = "/proxy/dvh/json/Hr/NOM2/NOM_HRORG_HIERARKI_v1";
    private static final String enheterV2URL = "/proxy/dvh/json/Hr/NOM2/NOM_HRORG_HIERARKI2_v2";

    private static final List<String> allUrls = List.of(stillingerURL, ressurserURL, enheterURL, enheterV2URL);

    @Test
    @DisplayName("Validerer gyldig respons med medsendt token")
    public void scenario11() {
        allUrls.forEach(this::scenario11Helper);
    }

    @SneakyThrows
    public void scenario11Helper(String url) {
        webTestClient
            .get()
            .uri(url)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getValidToken(oAuth2Server)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk();
    }

    @Test
    @DisplayName("Validerer 401 UNAUTHORIZED ved manglende token")
    public void scenario21() {
        allUrls.forEach(this::scenario21Helper);
    }

    @SneakyThrows
    public void scenario21Helper(String url) {
        webTestClient
            .get()
            .uri(url)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isUnauthorized();
    }

    @Test
    @DisplayName("Validerer 401 UNAUTHORIZED ved ugyldig issuer i token")
    public void scenario22() {
        allUrls.forEach(this::scenario22Helper);
    }

    @SneakyThrows
    public void scenario22Helper(String url) {

        webTestClient
            .get()
            .uri(url)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getTokenWithUnknownIssuer(oAuth2Server)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isUnauthorized();
    }

    @Test
    @DisplayName("Validerer 401 UNAUTHORIZED ved ugyldig audience i token")
    public void scenario25() {
        allUrls.forEach(this::scenario25Helper);
    }

    @SneakyThrows
    public void scenario25Helper(String url) {

        webTestClient
            .get()
            .uri(url)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getTokenWithInvalidAudience(oAuth2Server)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isUnauthorized();
    }

    @Test
    @DisplayName("Validerer 401 UNAUTHORIZED ved utløpt token")
    public void scenario26() {
        allUrls.forEach(this::scenario26Helper);
    }

    @SneakyThrows
    public void scenario26Helper(String url) {

        webTestClient
            .get()
            .uri(url)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(getExpiredToken(oAuth2Server)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isUnauthorized();
    }
    
}
