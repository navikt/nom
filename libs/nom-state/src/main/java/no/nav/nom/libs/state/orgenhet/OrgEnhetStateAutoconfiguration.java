package no.nav.nom.libs.state.orgenhet;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
@Conditional(OrgEnhetStateAutoconfiguration.OrgEnhetStateEnabledCondition.class)
public class OrgEnhetStateAutoconfiguration extends DefaultStateAutoconfiguration<OrgEnhetState> {
    public OrgEnhetStateAutoconfiguration(Environment environment, @Value("${nom.libs.state.idle-event-interval-seconds}") int idleEventIntervalSeconds) {
        super(environment, idleEventIntervalSeconds);
    }

    @Bean(name = "nomStateOrgEnhetRepository")
    @ConditionalOnProperty(value = "nom.libs.state.orgenhet.enabled")
    OrgEnhetStateRepository nomStateOrgEnhetRepository() {
        return new OrgEnhetStateRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.orgenhet.enabled")
    OrgEnhetStateRepository orgEnhetRepository() {
        return new OrgEnhetStateRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.orgenhet.enabled")
    NomStateOrgEnhetListener nomStateOrgEnhetListener(
        @Value("${nom.libs.state.orgenhet.state-topic}") String stateTopic,
        @Qualifier("nomStateOrgEnhetRepository") OrgEnhetStateRepository orgEnhetStateRepository,
        String orgEnhetStateListenerId
    ) {
        log.info("Registering NomStateOrgEnhetListener with stateTopic: {}, listenerId: {}", stateTopic, orgEnhetStateListenerId);
        return new NomStateOrgEnhetListener(stateTopic, orgEnhetStateRepository, orgEnhetStateListenerId);
    }

    @Bean
    @ConditionalOnMissingBean(name = "nomStateOrgEnhetConsumerFactory")
    @ConditionalOnProperty(value = "nom.libs.state.orgenhet.enabled")
    ConsumerFactory<String, OrgEnhetState> nomStateOrgEnhetConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        return getDefaultOrgEnhetConsumerFactory(properties.buildConsumerProperties(), objectMapper);
    }

    static ConsumerFactory<String, OrgEnhetState> getDefaultOrgEnhetConsumerFactory(java. util. Map<String, Object> configs, ObjectMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(configs,
            new StringDeserializer(), new ErrorHandlingDeserializer<>(new JsonDeserializer<>(OrgEnhetState.class, objectMapper))
        );
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.orgenhet.enabled")
    ConcurrentKafkaListenerContainerFactory<String, OrgEnhetState> nomStateOrgEnhetKafkaListenerContainerFactory(
        @Qualifier("nomStateOrgEnhetConsumerFactory") ConsumerFactory<String, OrgEnhetState> consumerFactory
    ) {
        return defaultContainerFactorySettings(consumerFactory, new ConcurrentKafkaListenerContainerFactory<>());
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.orgenhet.enabled")
    public String orgEnhetStateListenerId(@Value("${nom.libs.state.orgenhet.listener-id}") String listenerId) {return getListenerIdString(listenerId); }

    static class OrgEnhetStateEnabledCondition extends AllNestedConditions {
        OrgEnhetStateEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "nom.libs.state.orgenhet.enabled")
        static class isEnabled {}
    }
}
