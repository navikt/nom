package no.nav.nom.libs.state.ressurs.mastered;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
@Conditional(MasteredRessursStateAutoconfiguration.MasteredRessursStateEnabledCondition.class)
public class MasteredRessursStateAutoconfiguration {
    final Environment environment;

    public MasteredRessursStateAutoconfiguration(Environment environment) {
        this.environment = environment;
    }

    @Bean(name = "nomStateMasteredRessursRepository")
    @ConditionalOnProperty(value = "nom.libs.state.mastered-ressurs.enabled")
    MasteredRessursStateRepository nomStateMasteredRessursRepository() {
        return new MasteredRessursStateRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.mastered-ressurs.enabled")
    MasteredRessursStateRepository masteredRessursRepository() {
        return new MasteredRessursStateRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.mastered-ressurs.enabled")
    NomStateMasteredRessursListener nomStateMasteredRessursListener(
        @Value("${nom.libs.state.mastered-ressurs.state-topic}") String stateTopic,
        @Qualifier("nomStateMasteredRessursRepository") MasteredRessursStateRepository masteredRessursStateRepository,
        String masteredRessursStateListenerId
    ) {
        log.info("Registering NomStateMasteredRessursListener with stateTopic: {}, listenerId: {}", stateTopic, masteredRessursStateListenerId);
        return new NomStateMasteredRessursListener(stateTopic, masteredRessursStateRepository, masteredRessursStateListenerId);
    }

    @Bean
    @ConditionalOnMissingBean(name = "nomStateMasteredRessursConsumerFactory") // NOTE: May be removed when old ConsumerFactory in nom-api is deleted.
    @ConditionalOnProperty(value = "nom.libs.state.mastered-ressurs.enabled")
    ConsumerFactory<String, LocalDateTime> nomStateMasteredRessursConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(
            properties.buildConsumerProperties(),
            new StringDeserializer(),
            new JsonDeserializer<>(LocalDateTime.class, objectMapper)
        );
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.mastered-ressurs.enabled")
    ConcurrentKafkaListenerContainerFactory<String, LocalDateTime> nomStateMasteredRessursKafkaListenerContainerFactory(
        @Qualifier("nomStateMasteredRessursConsumerFactory") ConsumerFactory<String, LocalDateTime> consumerFactory,
        @Value("${nom.libs.state.idle-event-interval-seconds}") int idleEventIntervalSeconds

    ) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, LocalDateTime>();
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
    @ConditionalOnProperty(value = "nom.libs.state.mastered-ressurs.enabled")
    public String masteredRessursStateListenerId(@Value("${nom.libs.state.mastered-ressurs.listener-id}") String listenerId) {
        if (isLocal()) {
            return listenerId + "-" + Math.abs(new Random().nextInt());
        }
        return listenerId;
    }

    private boolean isLocal() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        return profiles.contains("local") || profiles.contains("test");
    }

    static class MasteredRessursStateEnabledCondition extends AllNestedConditions {
        MasteredRessursStateEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "nom.libs.state.mastered-ressurs.enabled")
        static class isEnabled {}
    }
}
