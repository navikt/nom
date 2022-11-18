package no.nav.nom.apps.api.postgresql;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.Event;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@Configuration
public class EventsourceListenerConfig {
    public static final String GROUP_ID = "EventSourceBackupCatchUpJob";

    @Bean
    public ConsumerFactory<String, Event> eventsourceConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        final var jsonDeserializer = new JsonDeserializer<>(Event.class, objectMapper);

        jsonDeserializer.configure(
                Map.of(
                        JsonDeserializer.TRUSTED_PACKAGES, "no.nav.nom.contract.nom.eventsource"
                ), true
        );

        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(), new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Event>> eventsourceKafkaListenerContainerFactory(ConsumerFactory<String, Event> consumerFactory) {
        var concurrentKafkaListenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, Event>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(consumerFactory);

        return concurrentKafkaListenerContainerFactory;
    }
}
