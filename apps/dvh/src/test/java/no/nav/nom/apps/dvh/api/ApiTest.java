package no.nav.nom.apps.dvh.api;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.time.Instant;
import java.util.Date;

public class ApiTest extends ApiTestBase {
    private static final String stillingerURL = "/proxy/stillinger";
    private static final String enheterURL = "/proxy/enheter";
    private static final String ressurserURL = "/proxy/ressurser";

    @BeforeEach
    public void setup(){
        stubFor(get("/aad/discovery/v2.0/keys").willReturn(aResponse().withBody(String.format("{\"keys\":[%s]}", rsaKey.toPublicJWK().toString()))));
    }

    @Test
    @DisplayName("Validerer gyldig respons med medsendt token")
    public void scenario11(){
        scenario11Helper(stillingerURL);
        scenario11Helper(enheterURL);
        scenario11Helper(ressurserURL);
    }

    @SneakyThrows
    public void scenario11Helper(String url){
        val claims = securityTestUtils.validJwtClaims();
        val token = securityTestUtils.createToken(claims);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>("body", headers), String.class);
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer 401 UNAUTHORIZED ved manglende token")
    public void scenario21(){
        scenario21Helper(stillingerURL);
        scenario21Helper(enheterURL);
        scenario21Helper(ressurserURL);
    }

    @SneakyThrows
    public void scenario21Helper(String url){
        val result = restTemplate.exchange(URI.create(url), HttpMethod.GET, null, String.class);
        assertThat(result.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer 401 UNAUTHORIZED ved ikke signert token")
    public void scenario23(){
        scenario23Helper(stillingerURL);
        scenario23Helper(enheterURL);
        scenario23Helper(ressurserURL);
    }

    @SneakyThrows
    public void scenario23Helper(String url){
        val claims = securityTestUtils.validJwtClaims();
        val signer = new RSASSASigner(rsaKey);
        val signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("annenKey").build(), claims.build());
        signedJWT.sign(signer);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(signedJWT.serialize());

        val result = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>("body", headers), String.class);
        assertThat(result.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer 401 UNAUTHORIZED ved ugyldig issuer i token")
    public void scenario22(){
        scenario22Helper(stillingerURL);
        scenario22Helper(enheterURL);
        scenario22Helper(ressurserURL);
    }

    @SneakyThrows
    public void scenario22Helper(String url){
        val claims = securityTestUtils.validJwtClaims();
        claims.issuer("annenIssuer");
        val token = securityTestUtils.createToken(claims);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>("body", headers), String.class);
        assertThat(result.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer 403 FORBIDDEN ved ugyldig subject i token")
    public void scenario24(){
        scenario24Helper(stillingerURL);
        scenario24Helper(enheterURL);
        scenario24Helper(ressurserURL);
    }

    @SneakyThrows
    public void scenario24Helper(String url){
        val claims = securityTestUtils.validJwtClaims();
        claims.subject("ugyldigSubject");
        val token = securityTestUtils.createToken(claims);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>("body", headers), String.class);
        assertThat(result.getStatusCode(), is(HttpStatus.FORBIDDEN));
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer 401 UNAUTHORIZED ved ugyldig audience i token")
    public void scenario25(){
        scenario25Helper(stillingerURL);
        scenario25Helper(enheterURL);
        scenario25Helper(ressurserURL);
    }

    @SneakyThrows
    public void scenario25Helper(String url){
        val claims = securityTestUtils.validJwtClaims();
        claims.audience("ugyldigAudience");
        val token = securityTestUtils.createToken(claims);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>("body", headers), String.class);
        assertThat(result.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer 401 UNAUTHORIZED ved utløpt token")
    public void scenario26(){
        scenario26Helper(stillingerURL);
        scenario26Helper(enheterURL);
        scenario26Helper(ressurserURL);
    }

    @SneakyThrows
    public void scenario26Helper(String url){
        val claims = securityTestUtils.validJwtClaims();
        claims.expirationTime(Date.from(Instant.now().minusSeconds(60 * 60)));
        val token = securityTestUtils.createToken(claims);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>("body", headers), String.class);
        assertThat(result.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer 401 UNAUTHORIZED ved not before time frem i tid i tokenet")
    public void scenario27(){
        scenario27Helper(stillingerURL);
        scenario27Helper(enheterURL);
        scenario27Helper(ressurserURL);
    }

    @SneakyThrows
    public void scenario27Helper(String url){
        val claims = securityTestUtils.validJwtClaims();
        claims.notBeforeTime(Date.from(Instant.now().plusSeconds(60 * 60)));
        val token = securityTestUtils.createToken(claims);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        val result = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>("body", headers), String.class);
        assertThat(result.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
    }
}
