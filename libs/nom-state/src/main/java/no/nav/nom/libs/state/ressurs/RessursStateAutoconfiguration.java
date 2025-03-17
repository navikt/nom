package no.nav.nom.libs.state.ressurs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.RessursState;
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
@Conditional(RessursStateAutoconfiguration.RessursStateEnabledCondition.class)
public class RessursStateAutoconfiguration extends DefaultStateAutoconfiguration<RessursState> {
    public RessursStateAutoconfiguration(Environment environment, @Value("${nom.libs.state.idle-event-interval-seconds}") int idleEventIntervalSeconds) {
        super(environment, idleEventIntervalSeconds);
    }

    @Bean(name = "nomStateRessursRepository")
    @ConditionalOnProperty(value = "nom.libs.state.ressurs.enabled")
    RessursStateRepository nomStateRessursRepository() {
        return new RessursStateRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.ressurs.enabled")
    RessursStateRepository ressursRepository() {
        return new RessursStateRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "nom.libs.state.ressurs.enabled")
    NomStateRessursListener nomStateRessursListener(
        @Value("${nom.libs.state.ressurs.state-topic}") String stateTopic,
        @Qualifier("nomStateRessursRepository") RessursStateRepository ressursStateRepository,
        String ressursStateListenerId
    ) {
        log.info("Registering NomStateRessursListener with stateTopic: {}, listenerId: {}", stateTopic, ressursStateListenerId);
        return new NomStateRessursListener(stateTopic, ressursStateRepository, ressursStateListenerId);
    }

    @Bean
    @ConditionalOnMissingBean(name = "nomStateRessursConsumerFactory")
    @ConditionalOnProperty(value = "nom.libs.state.ressurs.enabled")
    ConsumerFactory<String, RessursState> nomStateRessursConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        return getDefaultRessursConsumerFactory(properties.buildConsumerProperties(), objectMapper);
    }

    static ConsumerFactory<String, RessursState> getDefaultRessursConsumerFactory(java. util. Map<String, Object> configs, ObjectMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(configs,
            new StringDeserializer(), new ErrorHandlingDeserializer<>(new JsonDeserializer<>(RessursState.class, objectMapper))
        );
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.ressurs.enabled")
    ConcurrentKafkaListenerContainerFactory<String, RessursState> nomStateRessursKafkaListenerContainerFactory(
        @Qualifier("nomStateRessursConsumerFactory") ConsumerFactory<String, RessursState> consumerFactory
    ) {
        return defaultContainerFactorySettings(consumerFactory, new ConcurrentKafkaListenerContainerFactory<>());
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.ressurs.enabled")
    public String ressursStateListenerId(@Value("${nom.libs.state.ressurs.listener-id}") String listenerId) { return getUniqueListenerIdString(listenerId); }

    static class RessursStateEnabledCondition extends AllNestedConditions {
        RessursStateEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "nom.libs.state.ressurs.enabled")
        static class isEnabled {}
    }
}
