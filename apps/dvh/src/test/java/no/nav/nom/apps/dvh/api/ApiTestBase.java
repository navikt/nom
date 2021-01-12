package no.nav.nom.apps.dvh.api;

import com.nimbusds.jose.jwk.RSAKey;
import no.nav.nom.apps.dvh.TestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

public class ApiTestBase extends TestBase {
    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    SecurityTestUtils securityTestUtils;

    @Autowired
    RSAKey rsaKey;
}
