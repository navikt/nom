package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer;

import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
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
public class OrgtilknytningEventKafkaConfig {

    @Bean
    ProducerFactory<String, OrgtilknytningEvent> orgtilknytningEventProducerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        final var jsonSerializer = new JacksonJsonSerializer<OrgtilknytningEvent>(jsonMapper);
        properties.setClientId("orgtilknytningEventProducer");
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), jsonSerializer);
    }

    @Bean
    KafkaTemplate<String, OrgtilknytningEvent> orgtilknytningEventProducerTemplate(
            ProducerFactory<String, OrgtilknytningEvent> orgtilknytningEventProducerFactory, @Value("${app.kafka.orgtilknytning.eventsource-topic}") String eventsourceTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(orgtilknytningEventProducerFactory);
        eventsourceTemplate.setDefaultTopic(eventsourceTopic);

        return eventsourceTemplate;
    }
}
