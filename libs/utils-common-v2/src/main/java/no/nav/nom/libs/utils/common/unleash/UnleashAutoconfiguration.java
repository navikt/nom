package no.nav.nom.libs.utils.common.unleash;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.strategy.Strategy;
import io.getunleash.util.UnleashConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;


@AutoConfiguration
@ConditionalOnClass(Unleash.class)
@ConditionalOnProperty(name = "unleash.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(UnleashPropeties.class)
public class UnleashAutoconfiguration {

    final Environment environment;

    public UnleashAutoconfiguration(Environment environment) {
        this.environment = environment;
    }

    @Bean
    @ConditionalOnMissingBean
    StrategySupplier strategies() {
        return () -> new Strategy[]{new ByClusterStrategy(), new ByUserIdStrategy(environment)};
    }

    @Bean
    @ConditionalOnMissingBean
    AppNameSupplier defaultAppNameSupplier(Environment environment) {
        return () -> environment.getRequiredProperty("app.name");
    }

    @Bean
    @ConditionalOnMissingBean
    public UnleashConfig unleashConfig(UnleashPropeties propeties, AppNameSupplier nameSupplier) {
        return UnleashConfig.builder()
            .appName(nameSupplier.name())
            .unleashAPI(propeties.endpoint().url())
            .build();
    }

    @Bean(name = "unleash")
    @ConditionalOnMissingBean
    public Unleash unleash(UnleashConfig unleashConfig, StrategySupplier supplier) {
        return new DefaultUnleash(unleashConfig, supplier.strategies());
    }
}