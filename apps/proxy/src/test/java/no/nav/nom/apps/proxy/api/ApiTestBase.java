package no.nav.nom.apps.proxy.api;

import com.nimbusds.jose.jwk.RSAKey;
import no.nav.nom.apps.dvh.TestBase;
import no.nav.nom.apps.proxy.common.AppProperties;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

public class ApiTestBase extends TestBase {

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected SecurityTestUtils securityTestUtils;

    @Autowired
    AppProperties appProperties;

    @Autowired
    RSAKey rsaKey;

}
