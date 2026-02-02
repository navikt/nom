package no.nav.nom.libs.state.authorize;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class DelegertLederStateAutoconfigurationTest {

    @Configuration
    static class TestMockBeans {
        @Bean
        KafkaProperties kafkaProperties() {
            return new KafkaProperties();
        }

        @Bean
        JsonMapper jsonMapper() {
            return new JsonMapper();
        }

        @Bean
        Environment environment() {
            return new StandardEnvironment();
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DelegertLederStateAutoconfiguration.class))
        .withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO));

    @Test
    void test_no_beans_present_if_state_delegert_leder_disabled() {
        contextRunner
            .withPropertyValues("nom.libs.state.delegert-leder.enabled=false")
            .run(
                context -> {
                    assertThat(context).doesNotHaveBean(NomStateDelegertLederListener.class);
                    assertThat(context).doesNotHaveBean(DelegertLederStateRepository.class);
                    assertThat(context).doesNotHaveBean("nomStateDelegertLederConsumerFactory");
                    assertThat(context).doesNotHaveBean("nomStateDelegertLederKafkaListenerContainerFactory");
                }
            );
    }

    @Test
    void test_all_beans_present_if_state_delegert_leder_enabled() {
        contextRunner
            .withUserConfiguration(TestMockBeans.class)
            .withPropertyValues("nom.libs.state.delegert-leder.enabled=true")
            .withPropertyValues("nom.libs.state.idle-event-interval-seconds=5")
            .run(
                context -> {
                    assertThat(context).hasSingleBean(NomStateDelegertLederListener.class);
                    assertThat(context).hasSingleBean(DelegertLederStateRepository.class);
                    assertThat(context).hasBean("nomStateDelegertLederConsumerFactory");
                    assertThat(context).hasBean("nomStateDelegertLederKafkaListenerContainerFactory");
                }
            );
    }
}
