package no.nav.nom.apps.api;

import org.springframework.context.annotation.Import;

@Import({SecurityTestUtils.class, ApiStarter.class})
class TestConfig {

}
