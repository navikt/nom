package no.nav.nom.libs.state.authorize;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@AutoConfiguration
@Conditional(DelegertLederStateAutoconfiguration.DelegertLederStateEnabledCondition.class)
public class DelegertLederStateAutoconfiguration {
    final Environment environment;

    public DelegertLederStateAutoconfiguration(Environment environment) {
        this.environment = environment;
    }

    @Bean(name = "nomStateDelegertLederRepository")
    @ConditionalOnProperty(value = "nom.libs.state.delegert-leder.enabled")
    DelegertLederStateRepository nomStateDelegertLederRepository() {
        return new DelegertLederStateRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.delegert-leder.enabled")
    DelegertLederStateRepository delegertLederRepository() {
        return new DelegertLederStateRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.delegert-leder.enabled")
    NomStateDelegertLederListener nomStateDelegertLederListener(
        @Value("${nom.libs.state.delegert-leder.state-topic}") String stateTopic,
        @Qualifier("nomStateDelegertLederRepository") DelegertLederStateRepository delegertLederStateRepository,
        String delegertLederStateListenerId
    ) {
        log.info("Registering NomStateDelegertLederListener with stateTopic: {}, listenerId: {}", stateTopic, delegertLederStateListenerId);
        return new NomStateDelegertLederListener(stateTopic, delegertLederStateRepository, delegertLederStateListenerId);
    }

    @Bean
    @ConditionalOnMissingBean(name = "nomStateDelegertLederConsumerFactory") // NOTE: May be removed when old ConsumerFactory in nom-api is deleted.
    @ConditionalOnProperty(value = "nom.libs.state.delegert-leder.enabled")
    ConsumerFactory<String, AuthorizationEvent> nomStateDelegertLederConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(
            properties.buildConsumerProperties(),
            new StringDeserializer(),
            new JsonDeserializer<>(AuthorizationEvent.class, objectMapper)
        );
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.delegert-leder.enabled")
    ConcurrentKafkaListenerContainerFactory<String, AuthorizationEvent> nomStateDelegertLederKafkaListenerContainerFactory(
        @Qualifier("nomStateDelegertLederConsumerFactory") ConsumerFactory<String, AuthorizationEvent> consumerFactory,
        @Value("${nom.libs.state.idle-event-interval-seconds}") int idleEventIntervalSeconds

    ) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, AuthorizationEvent>();
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
    @ConditionalOnProperty(value = "nom.libs.state.delegert-leder.enabled")
    public String delegertLederStateListenerId(@Value("${nom.libs.state.delegert-leder.listener-id}") String listenerId) {
        if (isLocal()) {
            return listenerId + "-" + Math.abs(new Random().nextInt());
        }
        return listenerId;
    }

    private boolean isLocal() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        return profiles.contains("local") || profiles.contains("test");
    }

    static class DelegertLederStateEnabledCondition extends AllNestedConditions {
        DelegertLederStateEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "nom.libs.state.delegert-leder.enabled")
        static class isEnabled {}
    }
}
