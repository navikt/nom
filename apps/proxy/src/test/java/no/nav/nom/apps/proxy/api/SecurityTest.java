package no.nav.nom.apps.proxy.api;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


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
        val claims = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(claims);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>("body", headers), String.class);
        assertThat(result.getStatusCode()).as("URL: %s", url) .isEqualTo(HttpStatus.OK);
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer 401 UNAUTHORIZED ved manglende token")
    public void scenario21() {
        allUrls.forEach(this::scenario21Helper);
    }

    @SneakyThrows
    public void scenario21Helper(String url) {
        val result = restTemplate.exchange(URI.create(url), HttpMethod.GET, null, String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer 401 UNAUTHORIZED ved ikke signert token")
    public void scenario23() {
        allUrls.forEach(this::scenario23Helper);
    }

    @SneakyThrows
    public void scenario23Helper(String url) {
        val claims = securityTestUtils.validJwtClaims();
        val signer = new RSASSASigner(rsaKey);
        val signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("annenKey").build(), claims.build());
        signedJWT.sign(signer);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(signedJWT.serialize());

        val result = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>("body", headers), String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer 401 UNAUTHORIZED ved ugyldig issuer i token")
    public void scenario22() {
        allUrls.forEach(this::scenario22Helper);
    }

    @SneakyThrows
    public void scenario22Helper(String url) {
        val claims = securityTestUtils.validJwtClaims();
        claims.issuer("annenIssuer");
        val token = securityTestUtils.createToken(claims);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>("body", headers), String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer 401 UNAUTHORIZED ved ugyldig audience i token")
    public void scenario25() {
        allUrls.forEach(this::scenario25Helper);
    }

    @SneakyThrows
    public void scenario25Helper(String url) {
        val claims = securityTestUtils.validJwtClaims();
        claims.audience("ugyldigAudience");
        val token = securityTestUtils.createToken(claims);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>("body", headers), String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer 401 UNAUTHORIZED ved utløpt token")
    public void scenario26() {
        allUrls.forEach(this::scenario26Helper);
    }

    @SneakyThrows
    public void scenario26Helper(String url) {
        val claims = securityTestUtils.validJwtClaims();
        claims.expirationTime(Date.from(Instant.now().minusSeconds(60 * 60)));
        val token = securityTestUtils.createToken(claims);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>("body", headers), String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer 401 UNAUTHORIZED ved not before time frem i tid i tokenet")
    public void scenario27() {
        allUrls.forEach(this::scenario27Helper);
    }

    @SneakyThrows
    public void scenario27Helper(String url) {
        val claims = securityTestUtils.validJwtClaims();
        claims.notBeforeTime(Date.from(Instant.now().plusSeconds(60 * 60)));
        val token = securityTestUtils.createToken(claims);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>("body", headers), String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
