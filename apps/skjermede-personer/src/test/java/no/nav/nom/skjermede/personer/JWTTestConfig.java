package no.nav.nom.skjermede.personer;

import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever;
import no.nav.security.token.support.test.JwkGenerator;
import no.nav.security.token.support.test.spring.TokenGeneratorController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Configuration
@Import({TokenGeneratorController.class})
public class JWTTestConfig {

    @Bean
    @Primary
    ProxyAwareResourceRetriever overrideOidcResourceRetriever() {
        return new CustomFileResourceRetriever();
    }

    @Bean
    JwkGenerator jwkGenerator() {
        return new JwkGenerator();
    }
}
