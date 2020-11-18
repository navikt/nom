package no.nav.nom.skjermede.personer.common.security;

import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("!local")
@Configuration
public class TokenSupportConfig {

    @Bean
    @Primary
    public ProxyAwareResourceRetriever proxyAwareResourceRetriever() {
        return new ProxyAwareResourceRetriever();
    }
}
