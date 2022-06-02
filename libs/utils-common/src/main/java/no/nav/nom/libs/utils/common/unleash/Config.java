package no.nav.nom.libs.utils.common.unleash;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.strategy.Strategy;
import io.getunleash.util.UnleashConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Value("${unleash.endpoint.url}")
    private String endpoint;

    @Value("${app.name}")
    private String appName;

    @Bean
    public UnleashConfig unleashConfig() {
        return UnleashConfig.builder()
                .appName(appName)
                .unleashAPI(endpoint)
                .build();
    }

    @Bean(name = "unleash")
    @Autowired
    public Unleash defaultUnleash(UnleashConfig unleashConfig) {
        Strategy[] strategies = {
                new ByClusterStrategy(),
        };
        return new DefaultUnleash(unleashConfig, strategies);
    }
}