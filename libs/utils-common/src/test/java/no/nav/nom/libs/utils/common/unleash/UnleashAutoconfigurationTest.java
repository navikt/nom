package no.nav.nom.libs.utils.common.unleash;

import io.getunleash.DefaultUnleash;
import io.getunleash.FakeUnleash;
import io.getunleash.Unleash;
import io.getunleash.strategy.Strategy;
import io.getunleash.strategy.UserWithIdStrategy;
import io.getunleash.util.UnleashConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

public class UnleashAutoconfigurationTest {

    @Configuration
    static class EmptyStrategySupplierConfig {
        @Bean
        StrategySupplier supplier() {
            return () -> new Strategy[0];
        }
    }

    @Configuration
    static class DoubleStrategySupplierConfig {
        @Bean
        StrategySupplier supplier() {
            return () -> new Strategy[]{new ByClusterStrategy(), new UserWithIdStrategy()};
        }
    }

    @Configuration
    static class UserDefinedUnleashConfig {
        public static String APP_NAME = "test-user-defined-unleashconfig";

        @Bean
        UnleashConfig unleashConfig(UnleashPropeties properties) {
            return UnleashConfig.builder()
                .unleashAPI(properties.endpoint().url())
                .appName(APP_NAME)
                .build();
        }
    }

    @Configuration
    static class UserDefinedUnleash {
        @Bean
        Unleash unleash() {
            return new FakeUnleash();
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(UnleashAutoconfiguration.class))
        .withPropertyValues("app.name=unleash-test-app")
        .withInitializer(new ConditionEvaluationReportLoggingListener(LogLevel.INFO));

    @Test
    void test_no_beans_present_if_unleash_not_on_classpath() {
        contextRunner
            .withClassLoader(new FilteredClassLoader(Unleash.class))
            .run(
                context -> {
                    assertThat(context).doesNotHaveBean(UnleashPropeties.class);
                    assertThat(context).doesNotHaveBean(UnleashConfig.class);
                    assertThat(context).doesNotHaveBean(StrategySupplier.class);
                    assertThat(context).doesNotHaveBean(Unleash.class);
                }
            );
    }

    @Test
    void test_no_beans_present_if_unleash_disabled() {
        contextRunner
            .withPropertyValues("unleash.enabled=false")
            .run(
                context -> {
                    assertThat(context).doesNotHaveBean(UnleashPropeties.class);
                    assertThat(context).doesNotHaveBean(UnleashConfig.class);
                    assertThat(context).doesNotHaveBean(StrategySupplier.class);
                    assertThat(context).doesNotHaveBean(Unleash.class);
                }
            );
    }

    @Test
    void test_all_beans_present_given_unleash_endpoint_url() {
        contextRunner
            .withPropertyValues("unleash.endpoint.url=https://example.com")
            .run(
                context -> {
                    assertThat(context).hasSingleBean(UnleashPropeties.class);
                    assertThat(context).hasSingleBean(UnleashConfig.class);
                    assertThat(context).hasSingleBean(StrategySupplier.class);
                    assertThat(context).hasSingleBean(Unleash.class);
                }
            );
    }

    @Test
    void test_wiring_of_empty_user_created_strategysupplier() {
        contextRunner
            .withPropertyValues("unleash.endpoint.url=https://example.com")
            .withUserConfiguration(EmptyStrategySupplierConfig.class)
            .run(
                context -> {
                    assertThat(context).hasSingleBean(StrategySupplier.class);
                    assertThat(context.getBean(StrategySupplier.class).strategies()).hasSize(0);

                    assertThat(context).hasSingleBean(UnleashPropeties.class);
                    assertThat(context).hasSingleBean(UnleashConfig.class);
                    assertThat(context).hasSingleBean(Unleash.class);
                }
            );
    }


    @Test
    void test_wiring_of_user_created_strategysupplier_returning_two_strategies() {
        contextRunner
            .withPropertyValues("unleash.endpoint.url=https://example.com")
            .withUserConfiguration(DoubleStrategySupplierConfig.class)
            .run(
                context -> {
                    assertThat(context).hasSingleBean(StrategySupplier.class);
                    assertThat(context.getBean(StrategySupplier.class).strategies()).hasSize(2);

                    assertThat(context).hasSingleBean(UnleashPropeties.class);
                    assertThat(context).hasSingleBean(UnleashConfig.class);
                    assertThat(context).hasSingleBean(Unleash.class);
                }
            );
    }


    @Test
    void test_wiring_of_user_created_unleashconfig() {
        contextRunner
            .withPropertyValues("unleash.endpoint.url=https://example.com")
            .withUserConfiguration(UserDefinedUnleashConfig.class)
            .run(
                context -> {
                    assertThat(context).hasSingleBean(UnleashConfig.class);
                    assertThat(context.getBean(UnleashConfig.class).getAppName()).isEqualTo(UserDefinedUnleashConfig.APP_NAME);

                    assertThat(context).hasSingleBean(UnleashPropeties.class);
                    assertThat(context).hasSingleBean(StrategySupplier.class);
                    assertThat(context).hasSingleBean(Unleash.class);
                }
            );
    }

    @Test
    void test_wiring_of_user_created_unleash() {
        contextRunner
            .withPropertyValues("unleash.endpoint.url=https://example.com")
            .withUserConfiguration(UserDefinedUnleash.class)
            .run(
                context -> {
                    assertThat(context).hasSingleBean(Unleash.class);
                    assertThat(context.getBean(Unleash.class)).isInstanceOf(FakeUnleash.class);
                    assertThat(context.getBean(Unleash.class)).isNotInstanceOf(DefaultUnleash.class);

                    assertThat(context).hasSingleBean(UnleashPropeties.class);
                    assertThat(context).hasSingleBean(StrategySupplier.class);
                    assertThat(context).hasSingleBean(UnleashConfig.class);
                }
            );
    }

    @Test
    void test_exception_thrown_when_unleash_endpoint_url_is_absent() {
        contextRunner
            .run(
                context -> assertThat(context.getStartupFailure()).isInstanceOf(BeanCreationException.class)
            );
    }

    @Test
    void test_no_exception_thrown_when_unleash_endpoint_url_is_absent_and_unleash_is_disabled() {
        contextRunner
            .withPropertyValues("unleash.enabled=false")
            .run(
                context -> {
                    assertThat(context.getStartupFailure()).isNull();

                    assertThat(context).doesNotHaveBean(UnleashPropeties.class);
                    assertThat(context).doesNotHaveBean(UnleashConfig.class);
                    assertThat(context).doesNotHaveBean(StrategySupplier.class);
                    assertThat(context).doesNotHaveBean(Unleash.class);
                }
            );
    }

}
