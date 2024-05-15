package no.nav.nom.libs.state.orgtilknytning;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
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
@Conditional(OrgtilknytningStateAutoconfiguration.OrgtilknytningStateEnabledCondition.class)
public class OrgtilknytningStateAutoconfiguration {
    final Environment environment;

    public OrgtilknytningStateAutoconfiguration(Environment environment) {
        this.environment = environment;
    }

    @Bean(name = "nomStateOrgtilknytningRepository")
    @ConditionalOnProperty(value = "nom.libs.state.orgtilknytning.enabled")
    OrgtilknytningStateRepository nomStateOrgtilknytningRepository() {
        return new OrgtilknytningStateRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.orgtilknytning.enabled")
    OrgtilknytningStateRepository orgtilknytningRepository() {
        return new OrgtilknytningStateRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.orgtilknytning.enabled")
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener(
        @Value("${nom.libs.state.orgtilknytning.state-topic}") String stateTopic,
        @Qualifier("nomStateOrgtilknytningRepository") OrgtilknytningStateRepository orgtilknytningStateRepository,
        String orgtilknytningStateListenerId
    ) {
        log.info("Registering NomStateOrgtilknytningListener with stateTopic: {}, listenerId: {}", stateTopic, orgtilknytningStateListenerId);
        return new NomStateOrgtilknytningListener(stateTopic, orgtilknytningStateRepository, orgtilknytningStateListenerId);
    }

    @Bean
    @ConditionalOnMissingBean(name = "nomStateOrgtilknytningConsumerFactory") // NOTE: May be removed when old ConsumerFactory in nom-api is deleted.
    @ConditionalOnProperty(value = "nom.libs.state.orgtilknytning.enabled")
    ConsumerFactory<String, OrgtilknytningState> nomStateOrgtilknytningConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(
            properties.buildConsumerProperties(),
            new StringDeserializer(),
            new JsonDeserializer<>(OrgtilknytningState.class, objectMapper)
        );
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.orgtilknytning.enabled")
    ConcurrentKafkaListenerContainerFactory<String, OrgtilknytningState> nomStateOrgtilknytningKafkaListenerContainerFactory(
        @Qualifier("nomStateOrgtilknytningConsumerFactory") ConsumerFactory<String, OrgtilknytningState> consumerFactory,
        @Value("${nom.libs.state.idle-event-interval-seconds}") int idleEventIntervalSeconds

    ) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, OrgtilknytningState>();
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
    @ConditionalOnProperty(value = "nom.libs.state.orgtilknytning.enabled")
    public String orgtilknytningStateListenerId(@Value("${nom.libs.state.orgtilknytning.listener-id}") String listenerId) {
        if (isLocal()) {
            return listenerId + "-" + Math.abs(new Random().nextInt());
        }
        return listenerId;
    }

    private boolean isLocal() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        return profiles.contains("local") || profiles.contains("test");
    }

    static class OrgtilknytningStateEnabledCondition extends AllNestedConditions {
        OrgtilknytningStateEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "nom.libs.state.orgtilknytning.enabled")
        static class isEnabled {}
    }

}
