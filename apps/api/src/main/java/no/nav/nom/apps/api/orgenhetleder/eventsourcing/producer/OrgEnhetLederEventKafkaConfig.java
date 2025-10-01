package no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederEvent;
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
public class OrgEnhetLederEventKafkaConfig {

    @Bean
    ProducerFactory<String, OrgEnhetLederEvent> orgEnhetLederEventProducerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        final var jsonSerializer = new JsonSerializer<OrgEnhetLederEvent>(objectMapper);
        properties.setClientId("orgEnhetLederEventProducer");
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), jsonSerializer);
    }

    @Bean
    KafkaTemplate<String, OrgEnhetLederEvent> orgEnhetLederEventProducerTemplate(
        ProducerFactory<String, OrgEnhetLederEvent> orgEnhetLederEventProducerFactory, @Value("${app.kafka.orgEnhetLeder.eventsource-topic}") String eventsourceTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(orgEnhetLederEventProducerFactory);
        eventsourceTemplate.setDefaultTopic(eventsourceTopic);
        return eventsourceTemplate;
    }
}
