package no.nav.nom.skjermede.personer;

import no.nav.nom.skjermede.personer.common.CustomFileResourceRetriever;
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever;
import no.nav.security.token.support.test.spring.TokenGeneratorController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Configuration
@Import({TokenGeneratorController.class})
public class JWTTokenProvderTestConfig {

    @Bean
    @Primary
    ProxyAwareResourceRetriever overrideOidcResourceRetriever() {
        return new CustomFileResourceRetriever();
    }
}
