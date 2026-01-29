package no.nav.nom.libs.state.orgenhet;

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

public class OrgEnhetStateAutoconfigurationTest {

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
        .withConfiguration(AutoConfigurations.of(OrgEnhetStateAutoconfiguration.class))
        .withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO));

    @Test
    void test_no_beans_present_if_state_orgenhet_disabled() {
        contextRunner
            .withPropertyValues("nom.libs.state.orgenhet.enabled=false")
            .run(
                context -> {
                    assertThat(context).doesNotHaveBean(NomStateOrgEnhetListener.class);
                    assertThat(context).doesNotHaveBean(OrgEnhetStateRepository.class);
                    assertThat(context).doesNotHaveBean("nomStateOrgEnhetConsumerFactory");
                    assertThat(context).doesNotHaveBean("nomStateOrgEnhetKafkaListenerContainerFactory");
                }
            );
    }

    @Test
    void test_all_beans_present_if_state_orgenhet_enabled() {
        contextRunner
            .withUserConfiguration(TestMockBeans.class)
            .withPropertyValues("nom.libs.state.orgenhet.enabled=true")
            .withPropertyValues("nom.libs.state.idle-event-interval-seconds=5")
            .run(
                context -> {
                    assertThat(context).hasSingleBean(NomStateOrgEnhetListener.class);
                    assertThat(context).hasSingleBean(OrgEnhetStateRepository.class);
                    assertThat(context).hasBean("nomStateOrgEnhetConsumerFactory");
                    assertThat(context).hasBean("nomStateOrgEnhetKafkaListenerContainerFactory");
                }
            );
    }
}
