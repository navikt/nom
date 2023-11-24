package no.nav.nom.apps.api.organisering.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class OrganiseringEventKafkaConfig {

    @Bean
    ProducerFactory<String, OrganiseringEvent> organiseringEventProducerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        final var jsonSerializer = new JsonSerializer<OrganiseringEvent>(objectMapper);
        properties.setClientId("organiseringEventProducer");
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), jsonSerializer);
    }

    @Bean
    KafkaTemplate<String, OrganiseringEvent> organiseringEventProducerTemplate(
        ProducerFactory<String, OrganiseringEvent> organiseringEventProducerFactory,
        @Value("${app.kafka.organisering.eventsourceTopic}") String eventsourceTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(organiseringEventProducerFactory);
        eventsourceTemplate.setDefaultTopic(eventsourceTopic);

        return eventsourceTemplate;
    }
}
