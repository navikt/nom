package no.nav.nom.apps.api.ressurs.eventsourcing.producer;

import no.nav.nom.contract.nom.eventsource.RessursEvent;
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
public class RessursEventKafkaConfig {

    @Bean
    ProducerFactory<String, RessursEvent> ressursEventProducerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        final var jsonSerializer = new JacksonJsonSerializer<RessursEvent>(jsonMapper);
        properties.setClientId("ressursEventProducer");
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), jsonSerializer);
    }

    @Bean
    KafkaTemplate<String, RessursEvent> ressursEventProducerTemplate(
            ProducerFactory<String, RessursEvent> ressursEventProducerFactory,
            @Value("${app.kafka.ressurs.eventsourceTopic}") String eventsourceTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(ressursEventProducerFactory);
        eventsourceTemplate.setDefaultTopic(eventsourceTopic);

        return eventsourceTemplate;
    }
}
