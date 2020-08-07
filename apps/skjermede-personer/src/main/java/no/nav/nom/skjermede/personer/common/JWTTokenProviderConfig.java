package no.nav.nom.skjermede.personer.common;

import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local"})
@Import(JWTTokenProviderController.class)
public class JWTTokenProviderConfig {

    @Bean
    @Primary
    public ProxyAwareResourceRetriever overrideOidcResourceRetriever() {
        return new CustomFileResourceRetriever();
    }
}