package no.nav.nom.apps.nom.ressurser.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .exchangeStrategies(
                ExchangeStrategies.builder()
                    .codecs(codecConfigurer -> codecConfigurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                    .build())
            .build();
    }
}
