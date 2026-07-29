package no.nav.nom.libs.utils.common.unleash;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.UnleashContextProvider;
import io.getunleash.event.UnleashReady;
import io.getunleash.strategy.Strategy;
import io.getunleash.util.UnleashConfig;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
@AutoConfiguration
@ConditionalOnClass(Unleash.class)
@ConditionalOnProperty(name = "unleash.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(UnleashProperties.class)
public class UnleashAutoconfiguration {

    final Environment environment;

    public UnleashAutoconfiguration(Environment environment) {
        this.environment = environment;
    }

    @Bean
    @ConditionalOnMissingBean
    StrategySupplier strategies() {
        return () -> new Strategy[]{};
    }

    @Bean
    @ConditionalOnMissingBean
    AppNameSupplier defaultAppNameSupplier(Environment environment) {
        return () -> environment.getRequiredProperty("app.name");
    }

    @Bean
    @ConditionalOnMissingBean
    public UnleashConfig unleashConfig(
        UnleashProperties properties,
        AppNameSupplier nameSupplier,
        ContextSupplier contextSupplier,
        UnleashHealthSubscriber unleashHealthSubscriber
    ) {
        return UnleashConfig.builder()
            .appName(nameSupplier.name())
            .unleashAPI(properties.endpoint().url())
            .apiKey(properties.apiKey())
            .unleashContextProvider(contextSupplier.context())
            .subscriber(unleashHealthSubscriber)
            .build();
    }

    @Bean
    @ConditionalOnMissingBean
    ContextSupplier defaultContextSupplier() {
        return UnleashContextProvider::getDefaultProvider;
    }

    @Bean(name = "unleash")
    @ConditionalOnMissingBean
    public Unleash unleash(UnleashConfig unleashConfig, StrategySupplier supplier) {
        return new DefaultUnleash(unleashConfig, supplier.strategies());
    }

    @Bean(name = "unleashHealthCheckSubscriber")
    @ConditionalOnMissingBean
    private UnleashHealthSubscriber getUnleashHealthCheckSubscriber() {
        var healthCheckRequested = "true".equals(System.getenv("UNLEASH_SHOULD_PERFORM_HEALTH_CHECK"));
        var shouldBypassHealthCheck = !healthCheckRequested;
        return new UnleashHealthSubscriber(shouldBypassHealthCheck);
    }

    public static class UnleashHealthSubscriber implements io.getunleash.event.UnleashSubscriber, HealthIndicator{
        private final AtomicBoolean hasInitialized;

        public UnleashHealthSubscriber(boolean bypassHealthCheck) {
            this.hasInitialized = new AtomicBoolean(bypassHealthCheck);
            if(!bypassHealthCheck) {
                log.info("Unleash not healthy. Waiting for 'Unleash ready' event.");
            }
        }

        @Override
        public void onReady(@NonNull UnleashReady unleashReady) {
            log.info("'Unleash ready' event received. Unleash it now considered healthy.");
            this.hasInitialized.set(true);
        }

        @Override
        public @Nullable Health health(boolean includeDetails) {
            return this.hasInitialized.get() ? Health.up().build() : Health.down().build();
        }

        @Override
        public @Nullable Health health() {
            return this.health(false);
        }
    }
}