package no.nav.nom.skjermede.personer.common;

import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @Primary
    public ProxyAwareResourceRetriever proxyAwareResourceRetriever() {
        return new ProxyAwareResourceRetriever();
    }
}
