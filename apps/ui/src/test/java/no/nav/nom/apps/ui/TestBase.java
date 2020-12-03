package no.nav.nom.apps.ui;

import com.nimbusds.jose.jwk.RSAKey;
import com.thomaskasene.wiremock.junit.WireMockStubs;
import no.nav.nom.apps.ui.common.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;


@WireMockStubs
@ActiveProfiles({"test", "log-console"})
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class TestBase {

    @Autowired
    protected TestRestTemplate template;

    @Autowired
    protected SecurityTestUtils securityUtils;

    @Autowired
    protected RSAKey rsaKey;

    @BeforeEach
    public void testBaseBeforeEach() {
        stubFor(get("/aad/discovery/v2.0/keys").willReturn(aResponse().withBody(String.format("{\"keys\":[%s]}", rsaKey.toPublicJWK().toString()))));
    }
}
