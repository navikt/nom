package no.nav.nom.libs.state.orgtilknytning;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

public class OrgtilknytningStateAutoconfigurationTest {

    @Configuration
    static class TestMockBeans {
        @Bean
        KafkaProperties kafkaProperties() {
            return new KafkaProperties();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        Environment environment() {
            return new StandardEnvironment();
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(OrgtilknytningStateAutoconfiguration.class))
        .withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO));

    @Test
    void test_no_beans_present_if_state_orgtilknytning_disabled() {
        contextRunner
            .withPropertyValues("nom.libs.state.orgtilknytning.enabled=false")
            .run(
                context -> {
                    assertThat(context).doesNotHaveBean(NomStateOrgtilknytningListener.class);
                    assertThat(context).doesNotHaveBean(OrgtilknytningStateRepository.class);
                    assertThat(context).doesNotHaveBean("nomStateOrgtilknytningConsumerFactory");
                    assertThat(context).doesNotHaveBean("nomStateOrgtilknytningKafkaListenerContainerFactory");
                }
            );
    }

    @Test
    void test_all_beans_present_if_state_orgtilknytning_enabled() {
        contextRunner
            .withUserConfiguration(TestMockBeans.class)
            .withPropertyValues("nom.libs.state.orgtilknytning.enabled=true")
            .withPropertyValues("nom.libs.state.idle-event-interval-seconds=5")
            .run(
                context -> {
                    assertThat(context).hasSingleBean(NomStateOrgtilknytningListener.class);
                    assertThat(context).hasSingleBean(OrgtilknytningStateRepository.class);
                    assertThat(context).hasBean("nomStateOrgtilknytningConsumerFactory");
                    assertThat(context).hasBean("nomStateOrgtilknytningKafkaListenerContainerFactory");
                }
            );
    }
}
