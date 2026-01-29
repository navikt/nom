package no.nav.nom.apps.api.organisering.evtlistener;

import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;

@Configuration
public class OrganiseringEventSourceListenerConfig {

    @Bean
    ConsumerFactory<String, OrganiseringEvent> organiseringEventConsumerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        final var jsonDeserializer = new JacksonJsonDeserializer<>(OrganiseringEvent.class, jsonMapper);
        jsonDeserializer.configure(Map.of(JacksonJsonDeserializer.TRUSTED_PACKAGES, "no.nav.nom.contract.nom.event"), true);
        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(), new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, OrganiseringEvent> organiseringEventSourceKafkaListenerContainerFactory(ConsumerFactory<String, OrganiseringEvent> consumerFactory) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, OrganiseringEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);

        // If authentication towards kafka broker topic fails, retry every 10s
        factory.setContainerCustomizer(container ->
            container.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10))
        );

        return factory;
    }
}
