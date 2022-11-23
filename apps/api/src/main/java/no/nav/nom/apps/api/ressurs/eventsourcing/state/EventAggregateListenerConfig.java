package no.nav.nom.apps.api.ressurs.eventsourcing.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@Configuration
public class EventAggregateListenerConfig {
    @Bean
    ConsumerFactory<String, RessursState> consumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        final var jsonDeserializer = new JsonDeserializer<>(RessursState.class, objectMapper);

        jsonDeserializer.configure(
                Map.of(
                        JsonDeserializer.TRUSTED_PACKAGES, "no.nav.nom.contract.nom.event"
                ), true
        );

        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(), new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, RessursState> kafkaListenerContainerFactory(ConsumerFactory<String, RessursState> consumerFactory) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, RessursState>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        return factory;
    }
}
