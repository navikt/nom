package no.nav.nom.libs.state.ressurs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.RessursState;
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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@AutoConfiguration
@Conditional(RessursStateAutoconfiguration.RessursStateEnabledCondition.class)
public class RessursStateAutoconfiguration {
    final Environment environment;

    public RessursStateAutoconfiguration(Environment environment) {
        this.environment = environment;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.ressurs.enabled")
    RessursStateRepository nomStateRessursRepository() {
        return new RessursStateRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.ressurs.enabled")
    NomStateRessursListener nomStateRessursListener(
        @Value("${app.nom-state.kafka.ressurs.state-topic}") String stateTopic,
        RessursStateRepository ressursStateRepository,
        String ressursStateListenerId,
        KafkaProperties kafkaProperties
    ) {
        log.info("Registering NomStateRessursListener with stateTopic: {}, listenerId: {}", stateTopic, ressursStateListenerId);
        return new NomStateRessursListener(stateTopic, ressursStateRepository, ressursStateListenerId, kafkaProperties);
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.ressurs.enabled")
    ConsumerFactory<String, RessursState> nomStateRessursConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        final var jsonDeserializer = new JsonDeserializer<>(RessursState.class, objectMapper);

        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(), new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.ressurs.enabled")
    ConcurrentKafkaListenerContainerFactory<String, RessursState> nomStateRessursKafkaListenerContainerFactory(@Qualifier("nomStateRessursConsumerFactory") ConsumerFactory<String, RessursState> consumerFactory) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, RessursState>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setIdleEventInterval(10*1000L);

        // If authentication towards kafka broker topic fails, retry every 10s
        factory.setContainerCustomizer(container -> {
                container.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10));
                log.info(container.toString());
            }
        );

        return factory;
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.ressurs.enabled")
    public String ressursStateListenerId(@Value("${app.nom-state.kafka.ressurs.listener-id}") String listenerId) {
        if (isLocal()) {
            return listenerId + "-" + Math.abs(new Random().nextInt());
        }
        return listenerId;
    }

    private boolean isLocal() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        return profiles.contains("local") || profiles.contains("test");
    }

    static class RessursStateEnabledCondition extends AllNestedConditions {
        RessursStateEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "nom.libs.state.ressurs.enabled")
        static class isEnabled {}
    }
}
