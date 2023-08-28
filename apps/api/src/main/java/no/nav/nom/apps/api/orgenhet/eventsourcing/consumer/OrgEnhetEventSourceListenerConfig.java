package no.nav.nom.apps.api.orgenhet.eventsourcing.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.Duration;
import java.util.Map;

@Configuration
public class OrgEnhetEventSourceListenerConfig {

    @Bean
    ConsumerFactory<String, OrgEnhetEvent> orgEnhetEventConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        final var jsonDeserializer = new JsonDeserializer<>(OrgEnhetEvent.class, objectMapper);

        jsonDeserializer.configure(Map.of(JsonDeserializer.TRUSTED_PACKAGES, "no.nav.nom.contract.nom.event"), true);

        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(), new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, OrgEnhetEvent> orgEnhetEventSourceKafkaListenerContainerFactory(ConsumerFactory<String, OrgEnhetEvent> consumerFactory) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, OrgEnhetEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);

        // If authentication towards kafka broker topic fails, retry every 10s
        factory.setContainerCustomizer(container ->
            container.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10))
        );

        return factory;
    }
}
