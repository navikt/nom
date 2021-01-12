package no.nav.nom.apps.dvh.api;

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

public class ApiTest extends ApiTestBase {
    private static final String URL = "/proxy/stillinger";

    @BeforeEach
    public void setup(){
        stubFor(get("/aad/discovery/v2.0/keys").willReturn(aResponse().withBody(String.format("{\"keys\":[%s]}", rsaKey.toPublicJWK().toString()))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer gyldig respons med medsendt token")
    public void scenario11(){
        val result = restTemplate.exchange(URI.create(URL), HttpMethod.GET, getHeaders("test"), String.class);
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer 401 UNAUTHORIZED ved manglende token")
    public void scenario21(){
        val result = restTemplate.exchange(URI.create(URL), HttpMethod.GET, null, String.class);
        assertThat(result.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @SneakyThrows
    @DisplayName("Validerer 401 UNAUTHORIZED ved ugyldig audience i token")
    public void scenario22(){
        val result = restTemplate.exchange(URI.create(URL), HttpMethod.GET, getHeaders("feilAud"), String.class);
        assertThat(result.getStatusCode(), is(HttpStatus.UNAUTHORIZED));

    }


    private HttpEntity<String> getHeaders(String audience){
        val claims = securityTestUtils.validJwtClaims(audience);
        val token = securityTestUtils.createToken(claims);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        return new HttpEntity<>("body", headers);
    }
}
