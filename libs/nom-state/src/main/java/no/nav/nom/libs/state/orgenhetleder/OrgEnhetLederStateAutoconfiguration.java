package no.nav.nom.libs.state.orgenhetleder;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
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

import java.util.Map;

@Slf4j
@AutoConfiguration
@Conditional(OrgEnhetLederStateAutoconfiguration.OrgEnhetLederStateEnabledCondition.class)
public class OrgEnhetLederStateAutoconfiguration extends DefaultStateAutoconfiguration<OrgEnhetLederState> {

    public OrgEnhetLederStateAutoconfiguration(Environment environment, @Value("${nom.libs.state.idle-event-interval-seconds}") int idleEventIntervalSeconds) {
        super(environment, idleEventIntervalSeconds);
    }

    @Bean(name = "nomStateOrgEnhetLederRepository")
    @ConditionalOnProperty(value = "nom.libs.state.orgenhet-leder.enabled")
    OrgEnhetLederStateRepository nomStateOrgEnhetLederRepository() {
        return new OrgEnhetLederStateRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.orgenhet-leder.enabled")
    OrgEnhetLederStateRepository orgEnhetLederStateRepository() {
        return new OrgEnhetLederStateRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.orgenhet-leder.enabled")
    NomStateOrgEnhetLederListener nomStateOrgEnhetLederListener(
        @Value("${nom.libs.state.orgenhet-leder.state-topic}") String stateTopic,
        @Qualifier("nomStateOrgEnhetLederRepository") OrgEnhetLederStateRepository orgEnhetLederStateRepository,
        String orgEnhetLederStateListenerId
    ) {
        log.info("Registrering NomStateOrgEnhetLederListener with stateTopic: {}, listenerId: {}", stateTopic, orgEnhetLederStateListenerId);
        return new NomStateOrgEnhetLederListener(stateTopic, orgEnhetLederStateListenerId, orgEnhetLederStateRepository);
    }

    @Bean
    @ConditionalOnMissingBean(name = "nomStateOrgEnhetLederConsumerFactory")
    @ConditionalOnProperty(value = "nom.libs.state.orgenhet-leder.enabled")
    ConsumerFactory<String, OrgEnhetLederState> nomStateOrgEnhetLederConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        return getDefaultOrgEnhetLederConsumerFactory(properties.buildConsumerProperties(), objectMapper);
    }

    static ConsumerFactory<String, OrgEnhetLederState> getDefaultOrgEnhetLederConsumerFactory(Map<String, Object> configs, ObjectMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(configs,
            new StringDeserializer(), new ErrorHandlingDeserializer<>(new JsonDeserializer<>(OrgEnhetLederState.class, objectMapper))
        );
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.orgenhet-leder.enabled")
    ConcurrentKafkaListenerContainerFactory<String, OrgEnhetLederState> nomStateOrgEnhetLederKafkaListenerContainerFactory(
        @Qualifier("nomStateOrgEnhetLederConsumerFactory") ConsumerFactory<String, OrgEnhetLederState> consumerFactory
    ) {
        return defaultContainerFactorySettings(consumerFactory, new ConcurrentKafkaListenerContainerFactory<>());
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.orgenhet-leder.enabled")
    public String orgEnhetLederStateListenerId(@Value("${nom.libs.state.orgenhet-leder.listener-id}") String listenerId) { return getUniqueListenerIdString(listenerId); }

    static class OrgEnhetLederStateEnabledCondition extends AllNestedConditions {
        OrgEnhetLederStateEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "nom.libs.state.orgenhet-leder.enabled")
        static class isEnabled {}
    }
}
