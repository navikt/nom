package no.nav.nom.apps.proxy.api;

import com.nimbusds.jose.jwk.RSAKey;
import no.nav.nom.apps.SecurityTestUtils;
import no.nav.nom.apps.dvh.TestBase;
import no.nav.nom.apps.proxy.common.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

public class ApiTestBase extends TestBase {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    TestRestTemplate restTemplateDvh;

    @Autowired
    protected SecurityTestUtils securityTestUtils;

    @Autowired
    AppProperties appProperties;

    @Autowired
    RSAKey rsaKey;

}
