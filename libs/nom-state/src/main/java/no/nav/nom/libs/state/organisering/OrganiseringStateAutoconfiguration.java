package no.nav.nom.libs.state.organisering;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@AutoConfiguration
@Conditional(OrganiseringStateAutoconfiguration.OrganiseringStateEnabledCondition.class)
public class OrganiseringStateAutoconfiguration {
    final Environment environment;

    public OrganiseringStateAutoconfiguration(Environment environment) {
        this.environment = environment;
    }

    @Bean(name = "nomStateOrganiseringRepository")
    @ConditionalOnProperty(value = "nom.libs.state.organisering.enabled")
    OrganiseringStateRepository nomStateOrganiseringRepository() {
        return new OrganiseringStateRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.organisering.enabled")
    OrganiseringStateRepository organiseringRepository() {
        return new OrganiseringStateRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.organisering.enabled")
    NomStateOrganiseringListener nomStateOrganiseringListener(
        @Value("${nom.libs.state.organisering.state-topic}") String stateTopic,
        @Qualifier("nomStateOrganiseringRepository") OrganiseringStateRepository organiseringStateRepository,
        String organiseringStateListenerId
    ) {
        log.info("Registering NomStateOrganiseringListener with stateTopic: {}, listenerId: {}", stateTopic, organiseringStateListenerId);
        return new NomStateOrganiseringListener(stateTopic, organiseringStateRepository, organiseringStateListenerId);
    }

    @Bean
    @ConditionalOnMissingBean(name = "nomStateOrganiseringConsumerFactory") // NOTE: May be removed when old ConsumerFactory in nom-api is deleted.
    @ConditionalOnProperty(value = "nom.libs.state.organisering.enabled")
    ConsumerFactory<String, OrganiseringState> nomStateOrganiseringConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(
            properties.buildConsumerProperties(),
            new StringDeserializer(),
            new JsonDeserializer<>(OrganiseringState.class, objectMapper)
        );
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.organisering.enabled")
    ConcurrentKafkaListenerContainerFactory<String, OrganiseringState> nomStateOrganiseringKafkaListenerContainerFactory(
        @Qualifier("nomStateOrganiseringConsumerFactory") ConsumerFactory<String, OrganiseringState> consumerFactory,
        @Value("${nom.libs.state.idle-event-interval-seconds}") int idleEventIntervalSeconds

    ) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, OrganiseringState>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setIdleEventInterval(idleEventIntervalSeconds * 1000L);

        // If authentication towards kafka broker topic fails, retry every 10s
        factory.setContainerCustomizer(container -> {
                container.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10));
                log.info(container.toString());
            }
        );

        return factory;
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.organisering.enabled")
    public String organiseringStateListenerId(@Value("${nom.libs.state.organisering.listener-id}") String listenerId) {
        if (isLocal()) {
            return listenerId + "-" + Math.abs(new Random().nextInt());
        }
        return listenerId;
    }

    private boolean isLocal() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        return profiles.contains("local") || profiles.contains("test");
    }

    static class OrganiseringStateEnabledCondition extends AllNestedConditions {
        OrganiseringStateEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "nom.libs.state.organisering.enabled")
        static class isEnabled {}
    }

}
