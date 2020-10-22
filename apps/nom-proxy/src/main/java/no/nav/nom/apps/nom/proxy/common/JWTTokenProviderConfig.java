package no.nav.nom.apps.nom.proxy.common;

import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Profile({"local", "test"})
@Configuration
@Import(TokenGeneratorConfiguration.class)
public class JWTTokenProviderConfig {
}