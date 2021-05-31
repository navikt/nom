package no.nav.nom.apps.dvh;

import no.nav.nom.apps.proxy.ProxyStarter;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({SecurityTestUtils.class, ProxyStarter.class})
public class TestConfig {

}
