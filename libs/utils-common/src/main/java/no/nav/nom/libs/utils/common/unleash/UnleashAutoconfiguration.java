package no.nav.nom.libs.utils.common.unleash;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.strategy.Strategy;
import io.getunleash.util.UnleashConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;


@AutoConfiguration
@ConditionalOnClass(Unleash.class)
@ConditionalOnProperty(name = "unleash.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(UnleashPropeties.class)
public class UnleashAutoconfiguration {

    @Bean
    @ConditionalOnMissingBean
    StrategySupplier strategies() {
        return () -> new Strategy[]{new ByClusterStrategy()};
    }

    @Bean
    @ConditionalOnMissingBean
    public UnleashConfig unleashConfig(UnleashPropeties propeties, @Value("${app.name}") String appName) {
        return UnleashConfig.builder()
            .appName(appName)
            .unleashAPI(propeties.endpoint().url())
            .build();
    }

    @Bean(name = "unleash")
    @ConditionalOnMissingBean
    public Unleash unleash(UnleashConfig unleashConfig, StrategySupplier supplier) {
        return new DefaultUnleash(unleashConfig, supplier.strategies());
    }
}