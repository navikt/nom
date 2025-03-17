package no.nav.nom.libs.state.ressurs.mastered;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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

import java.time.LocalDateTime;

@Slf4j
@AutoConfiguration
@Conditional(MasteredRessursStateAutoconfiguration.MasteredRessursStateEnabledCondition.class)
public class MasteredRessursStateAutoconfiguration extends DefaultStateAutoconfiguration<LocalDateTime> {
    public MasteredRessursStateAutoconfiguration(Environment environment, @Value("${nom.libs.state.idle-event-interval-seconds}") int idleEventIntervalSeconds) {
        super(environment, idleEventIntervalSeconds);
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
    @ConditionalOnMissingBean(name = "nomStateMasteredRessursConsumerFactory")
    @ConditionalOnProperty(value = "nom.libs.state.mastered-ressurs.enabled")
    ConsumerFactory<String, LocalDateTime> nomStateMasteredRessursConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        return getDefaultMasteredRessursConsumerFactory(properties.buildConsumerProperties(), objectMapper);
    }

    static ConsumerFactory<String, LocalDateTime> getDefaultMasteredRessursConsumerFactory(java. util. Map<String, Object> configs, ObjectMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(configs,
            new StringDeserializer(), new ErrorHandlingDeserializer<>(new JsonDeserializer<>(LocalDateTime.class, objectMapper))
        );
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.mastered-ressurs.enabled")
    ConcurrentKafkaListenerContainerFactory<String, LocalDateTime> nomStateMasteredRessursKafkaListenerContainerFactory(
        @Qualifier("nomStateMasteredRessursConsumerFactory") ConsumerFactory<String, LocalDateTime> consumerFactory
    ) {
        return defaultContainerFactorySettings(consumerFactory, new ConcurrentKafkaListenerContainerFactory<>());
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.mastered-ressurs.enabled")
    public String masteredRessursStateListenerId(@Value("${nom.libs.state.mastered-ressurs.listener-id}") String listenerId) {return getUniqueListenerIdString(listenerId); }

    static class MasteredRessursStateEnabledCondition extends AllNestedConditions {
        MasteredRessursStateEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "nom.libs.state.mastered-ressurs.enabled")
        static class isEnabled {}
    }
}
