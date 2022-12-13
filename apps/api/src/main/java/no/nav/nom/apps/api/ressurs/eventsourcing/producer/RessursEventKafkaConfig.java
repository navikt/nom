package no.nav.nom.apps.api.ressurs.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class RessursEventKafkaConfig {

    @Bean
    ProducerFactory<String, RessursEvent> ressursEventProducerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        final var jsonSerializer = new JsonSerializer<RessursEvent>(objectMapper);

        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), jsonSerializer);
    }

    @Bean
    @Primary
    KafkaTemplate<String, RessursEvent> ressursEventProducerTemplate(
            ProducerFactory<String, RessursEvent> ressursEventProducerFactory,
            @Value("${app.kafka.ressurs.eventsourceTopic}") String eventsourceTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(ressursEventProducerFactory);
        eventsourceTemplate.setDefaultTopic(eventsourceTopic);

        return eventsourceTemplate;
    }

    @Bean(name="new_RessursEventSourceTopic")
    KafkaTemplate<String, RessursEvent> ressursEventProducerTemplate_newTopic(
            ProducerFactory<String, RessursEvent> ressursEventProducerFactory,
            @Value("${app.kafka.ressurs.newEventsourceTopic}") String eventsourceTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(ressursEventProducerFactory);
        eventsourceTemplate.setDefaultTopic(eventsourceTopic);

        return eventsourceTemplate;
    }
}
