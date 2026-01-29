package no.nav.nom.apps.rdbmsdata.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, RessursState> ressursStateConsumerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        return new DefaultKafkaConsumerFactory<>(
            properties.buildConsumerProperties(),
            new StringDeserializer(),
            new JacksonJsonDeserializer<>(RessursState.class, jsonMapper)
        );
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, RessursState>>
    ressursStateKafkaListenerContainerFactory(ConsumerFactory<String, RessursState> ressursEventConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, RessursState>();
        factory.setConsumerFactory(ressursEventConsumerFactory);
        factory.setBatchListener(true);
        factory.setContainerCustomizer(retryCustomizer());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, OrgEnhetState> orgenhetStateConsumerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        return new DefaultKafkaConsumerFactory<>(
            properties.buildConsumerProperties(),
            new StringDeserializer(),
            new JacksonJsonDeserializer<>(OrgEnhetState.class, jsonMapper)
        );
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, OrgEnhetState>>
    orgenhetStateKafkaListenerContainerFactory(ConsumerFactory<String, OrgEnhetState> orgenhetEventConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, OrgEnhetState>();
        factory.setConsumerFactory(orgenhetEventConsumerFactory);
        factory.setBatchListener(true);
        factory.setContainerCustomizer(retryCustomizer());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, OrganiseringState> organiseringStateConsumerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        return new DefaultKafkaConsumerFactory<>(
            properties.buildConsumerProperties(),
            new StringDeserializer(),
            new JacksonJsonDeserializer<>(OrganiseringState.class, jsonMapper)
        );
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, OrganiseringState>>
    organiseringStateKafkaListenerContainerFactory(ConsumerFactory<String, OrganiseringState> organiseringStateConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, OrganiseringState>();
        factory.setConsumerFactory(organiseringStateConsumerFactory);
        factory.setBatchListener(true);
        factory.setContainerCustomizer(retryCustomizer());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, OrgtilknytningState> orgtilknytningStateConsumerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        return new DefaultKafkaConsumerFactory<>(
            properties.buildConsumerProperties(),
            new StringDeserializer(),
            new JacksonJsonDeserializer<>(OrgtilknytningState.class, jsonMapper)
        );
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, OrgtilknytningState>>
    orgtilknytningStateKafkaListenerContainerFactory(ConsumerFactory<String, OrgtilknytningState> orgtilknytningStateConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, OrgtilknytningState>();
        factory.setConsumerFactory(orgtilknytningStateConsumerFactory);
        factory.setBatchListener(true);
        factory.setContainerCustomizer(retryCustomizer());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, OrgEnhetLederState> OrgEnhetLederStateConsumerFactory(KafkaProperties properties, JsonMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(
            properties.buildConsumerProperties(),
            new StringDeserializer(),
            new JacksonJsonDeserializer<>(OrgEnhetLederState.class, objectMapper)
        );
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, OrgEnhetLederState>>
    orgEnhetLederStateKafkaListenerContainerFactory(ConsumerFactory<String, OrgEnhetLederState> OrgEnhetLederStateConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, OrgEnhetLederState>();
        factory.setConsumerFactory(OrgEnhetLederStateConsumerFactory);
        factory.setBatchListener(true);
        factory.setContainerCustomizer(retryCustomizer());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, LocalDateTime> nomMasteredRessursConsumerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        return new DefaultKafkaConsumerFactory<>(
            properties.buildConsumerProperties(),
            new StringDeserializer(),
            new JacksonJsonDeserializer<>(LocalDateTime.class, jsonMapper)
        );
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, LocalDateTime>>
    nomMasteredRessursKafkaListenerContainerFactory(ConsumerFactory<String, LocalDateTime> nomMasteredRessursConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, LocalDateTime>();
        factory.setConsumerFactory(nomMasteredRessursConsumerFactory);
        factory.setBatchListener(true);
        factory.setContainerCustomizer(retryCustomizer());
        return factory;
    }

    // If authentication towards kafka broker topic fails, retry every 10s
    private static <T> ContainerCustomizer<String, T, ConcurrentMessageListenerContainer<String, T>> retryCustomizer() {
        return container -> {
            container.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10));
            log.info(container.toString());
        };
    }
}
