package no.nav.nom.libs.state.organisering;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.common.DefaultStateAutoconfiguration;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@AutoConfiguration
@Conditional(OrganiseringStateAutoconfiguration.OrganiseringStateEnabledCondition.class)
public class OrganiseringStateAutoconfiguration extends DefaultStateAutoconfiguration<OrganiseringState> {
    public OrganiseringStateAutoconfiguration(Environment environment, @Value("${nom.libs.state.idle-event-interval-seconds}") int idleEventIntervalSeconds) {
        super(environment, idleEventIntervalSeconds);
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
    @ConditionalOnMissingBean(name = "nomStateOrganiseringConsumerFactory")
    @ConditionalOnProperty(value = "nom.libs.state.organisering.enabled")
    ConsumerFactory<String, OrganiseringState> nomStateOrganiseringConsumerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        return getDefaultOrganiseringConsumerFactory(properties.buildConsumerProperties(), jsonMapper);
    }

    static ConsumerFactory<String, OrganiseringState> getDefaultOrganiseringConsumerFactory(java. util. Map<String, Object> configs, JsonMapper jsonMapper) {
        return new DefaultKafkaConsumerFactory<>(configs,
            new StringDeserializer(), new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(OrganiseringState.class, jsonMapper))
        );
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.organisering.enabled")
    ConcurrentKafkaListenerContainerFactory<String, OrganiseringState> nomStateOrganiseringKafkaListenerContainerFactory(
        @Qualifier("nomStateOrganiseringConsumerFactory") ConsumerFactory<String, OrganiseringState> consumerFactory
    ) {
        return defaultContainerFactorySettings(consumerFactory, new ConcurrentKafkaListenerContainerFactory<>());
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.organisering.enabled")
    public String organiseringStateListenerId(@Value("${nom.libs.state.organisering.listener-id}") String listenerId) {return getUniqueListenerIdString(listenerId); }

    static class OrganiseringStateEnabledCondition extends AllNestedConditions {
        OrganiseringStateEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "nom.libs.state.organisering.enabled")
        static class isEnabled {}
    }
}
