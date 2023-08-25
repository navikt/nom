package no.nav.nom.apps.api;

import no.nav.nom.libs.test.SecurityTestUtils;
import org.springframework.context.annotation.Import;

@Import({SecurityTestUtils.class, ApiStarter.class})
class TestConfig {

}
