package no.nav.nom.apps.nom.ressurser.common;

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile({"local", "test"})
@Configuration
@EnableMockOAuth2Server
public class JWTTokenProviderConfig {}