package no.nav.nom.apps.api.authorize.eventsourcing.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.Duration;

@Configuration
public class DelegertLederKafkaListenerConfig {

    @Bean
    ConsumerFactory<String, AuthorizationEvent> delegertLederConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        final var jsonDeserializer = new JsonDeserializer<>(AuthorizationEvent.class, objectMapper);

        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(), new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, AuthorizationEvent> delegertLederKafkaListenerContainerFactory(ConsumerFactory<String, AuthorizationEvent> consumerFactory) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, AuthorizationEvent>();
        factory.setConsumerFactory(consumerFactory);
//        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        // If authentication towards kafka broker topic fails, retry every 10s
        factory.setContainerCustomizer(container ->
            container.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10))
        );

        return factory;
    }
}
