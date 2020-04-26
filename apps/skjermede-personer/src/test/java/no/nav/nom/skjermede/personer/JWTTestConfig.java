package no.nav.nom.skjermede.personer;

import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(TokenGeneratorConfiguration.class)
public class JWTTestConfig {
}
