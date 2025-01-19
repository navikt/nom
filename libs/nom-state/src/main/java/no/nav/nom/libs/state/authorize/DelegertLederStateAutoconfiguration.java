package no.nav.nom.libs.state.authorize;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
import no.nav.nom.contract.nom.state.OrgEnhetState;
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
@Conditional(DelegertLederStateAutoconfiguration.DelegertLederStateEnabledCondition.class)
public class DelegertLederStateAutoconfiguration extends DefaultStateAutoconfiguration<AuthorizationEvent> {
    public DelegertLederStateAutoconfiguration(Environment environment, @Value("${nom.libs.state.idle-event-interval-seconds}") int idleEventIntervalSeconds) {
        super(environment, idleEventIntervalSeconds);
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
    @ConditionalOnMissingBean(name = "nomStateDelegertLederConsumerFactory")
    @ConditionalOnProperty(value = "nom.libs.state.delegert-leder.enabled")
    ConsumerFactory<String, AuthorizationEvent> nomStateDelegertLederConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        return getDefaultDelegertLederConsumerFactory(properties.buildConsumerProperties(), objectMapper);
    }

    static ConsumerFactory<String, AuthorizationEvent> getDefaultDelegertLederConsumerFactory(java. util. Map<String, Object> configs, ObjectMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(configs,
            new StringDeserializer(), new ErrorHandlingDeserializer<>(new JsonDeserializer<>(AuthorizationEvent.class, objectMapper))
        );
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.delegert-leder.enabled")
    ConcurrentKafkaListenerContainerFactory<String, AuthorizationEvent> nomStateDelegertLederKafkaListenerContainerFactory(
        @Qualifier("nomStateDelegertLederConsumerFactory") ConsumerFactory<String, AuthorizationEvent> consumerFactory
    ) {
        return defaultContainerFactorySettings(consumerFactory, new ConcurrentKafkaListenerContainerFactory<>());
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.delegert-leder.enabled")
    public String delegertLederStateListenerId(@Value("${nom.libs.state.delegert-leder.listener-id}") String listenerId) {return getListenerIdString(listenerId); }

    static class DelegertLederStateEnabledCondition extends AllNestedConditions {
        DelegertLederStateEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "nom.libs.state.delegert-leder.enabled")
        static class isEnabled {}
    }
}
