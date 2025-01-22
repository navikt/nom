package no.nav.nom.libs.state.orgtilknytning;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.common.DefaultStateAutoconfiguration;
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
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Slf4j
@AutoConfiguration
@Conditional(OrgtilknytningStateAutoconfiguration.OrgtilknytningStateEnabledCondition.class)
public class OrgtilknytningStateAutoconfiguration extends DefaultStateAutoconfiguration<OrgtilknytningState> {

    public OrgtilknytningStateAutoconfiguration(Environment environment, @Value("${nom.libs.state.idle-event-interval-seconds}") int idleEventIntervalSeconds) {
        super(environment, idleEventIntervalSeconds);
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
    @ConditionalOnMissingBean(name = "nomStateOrgtilknytningConsumerFactory")
    @ConditionalOnProperty(value = "nom.libs.state.orgtilknytning.enabled")
    ConsumerFactory<String, OrgtilknytningState> nomStateOrgtilknytningConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        return getDefaultOrgtilknytningConsumerFactory(properties.buildConsumerProperties(), objectMapper);
    }

    static ConsumerFactory<String, OrgtilknytningState> getDefaultOrgtilknytningConsumerFactory(java. util. Map<String, Object> configs, ObjectMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(configs,
            new StringDeserializer(), new ErrorHandlingDeserializer<>(new JsonDeserializer<>(OrgtilknytningState.class, objectMapper))
        );
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.orgtilknytning.enabled")
    ConcurrentKafkaListenerContainerFactory<String, OrgtilknytningState> nomStateOrgtilknytningKafkaListenerContainerFactory(
        @Qualifier("nomStateOrgtilknytningConsumerFactory") ConsumerFactory<String, OrgtilknytningState> consumerFactory
    ) {
        return defaultContainerFactorySettings(consumerFactory, new ConcurrentKafkaListenerContainerFactory<>());
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.orgtilknytning.enabled")
    public String orgtilknytningStateListenerId(@Value("${nom.libs.state.orgtilknytning.listener-id}") String listenerId) { return getListenerIdString(listenerId); }

    static class OrgtilknytningStateEnabledCondition extends AllNestedConditions {
        OrgtilknytningStateEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "nom.libs.state.orgtilknytning.enabled")
        static class isEnabled {}
    }
}
