package no.nav.nom.apps.api.orgenhet.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
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
public class OrgEnhetEventKafkaConfig {

    @Bean
    ProducerFactory<String, OrgEnhetEvent> orgEnhetEventProducerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        final var jsonSerializer = new JsonSerializer<OrgEnhetEvent>(objectMapper);
        properties.setClientId("orgEnhetEventProducer");
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), jsonSerializer);
    }

    @Bean
    KafkaTemplate<String, OrgEnhetEvent> orgEnhetEventProducerTemplate(
            ProducerFactory<String, OrgEnhetEvent> orgEnhetEventProducerFactory, @Value("${app.kafka.orgEnhet.eventsourceTopic}") String eventsourceTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(orgEnhetEventProducerFactory);
        eventsourceTemplate.setDefaultTopic(eventsourceTopic);

        return eventsourceTemplate;
    }
}
