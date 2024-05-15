package no.nav.nom.libs.state.orgenhet;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgEnhetState;
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
@Conditional(OrgEnhetStateAutoconfiguration.OrgEnhetStateEnabledCondition.class)
public class OrgEnhetStateAutoconfiguration {
    final Environment environment;

    public OrgEnhetStateAutoconfiguration(Environment environment) {
        this.environment = environment;
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
    @ConditionalOnMissingBean(name = "nomStateOrgEnhetConsumerFactory") // NOTE: May be removed when old ConsumerFactory in nom-api is deleted.
    @ConditionalOnProperty(value = "nom.libs.state.orgenhet.enabled")
    ConsumerFactory<String, OrgEnhetState> nomStateOrgEnhetConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(
            properties.buildConsumerProperties(),
            new StringDeserializer(),
            new JsonDeserializer<>(OrgEnhetState.class, objectMapper)
        );
    }

    @Bean
    @ConditionalOnProperty(value = "nom.libs.state.orgenhet.enabled")
    ConcurrentKafkaListenerContainerFactory<String, OrgEnhetState> nomStateOrgEnhetKafkaListenerContainerFactory(
        @Qualifier("nomStateOrgEnhetConsumerFactory") ConsumerFactory<String, OrgEnhetState> consumerFactory,
        @Value("${nom.libs.state.idle-event-interval-seconds}") int idleEventIntervalSeconds

    ) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, OrgEnhetState>();
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
    @ConditionalOnProperty(value = "nom.libs.state.orgenhet.enabled")
    public String orgEnhetStateListenerId(@Value("${nom.libs.state.orgenhet.listener-id}") String listenerId) {
        if (isLocal()) {
            return listenerId + "-" + Math.abs(new Random().nextInt());
        }
        return listenerId;
    }

    private boolean isLocal() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        return profiles.contains("local") || profiles.contains("test");
    }

    static class OrgEnhetStateEnabledCondition extends AllNestedConditions {
        OrgEnhetStateEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "nom.libs.state.orgenhet.enabled")
        static class isEnabled {}
    }

}
