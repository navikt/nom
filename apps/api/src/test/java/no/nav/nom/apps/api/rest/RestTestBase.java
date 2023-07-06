package no.nav.nom.apps.api.rest;

import no.nav.nom.apps.api.TestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

public abstract class RestTestBase extends TestBase {

    @Autowired
    protected WebTestClient webTestClient;
}
