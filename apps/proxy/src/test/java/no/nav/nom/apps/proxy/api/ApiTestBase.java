package no.nav.nom.apps.proxy.api;

import com.nimbusds.jose.jwk.RSAKey;
import no.nav.nom.apps.dvh.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

public class ApiTestBase extends TestBase {
    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    SecurityTestUtils securityTestUtils;

    @Autowired
    RSAKey rsaKey;

    @BeforeEach
    public void setup(){
        stubFor(get("/aad/discovery/v2.0/keys").willReturn(aResponse().withBody(String.format("{\"keys\":[%s]}", rsaKey.toPublicJWK().toString()))));
    }
}
