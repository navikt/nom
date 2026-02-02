package no.nav.nom.apps.api.authorize.eventsourcing.producer;

import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class AuthorizeEventKafkaConfig {

    @Bean
    ProducerFactory<String, AuthorizationEvent> authorizationEventProducerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        final var jsonSerializer = new JacksonJsonSerializer<AuthorizationEvent>(jsonMapper);
        properties.setClientId("authorizationEventProducer");
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), jsonSerializer);
    }

    @Bean
    KafkaTemplate<String, AuthorizationEvent> authorizationEventProducerTemplate(
            ProducerFactory<String, AuthorizationEvent> authorizationEventProducerFactory, @Value("${app.kafka.authorize.eventsource-topic}") String eventsourceTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(authorizationEventProducerFactory);
        eventsourceTemplate.setDefaultTopic(eventsourceTopic);
        return eventsourceTemplate;
    }
}
