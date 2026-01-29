package no.nav.nom.apps.api.ressurs.masterdata;

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

import java.time.LocalDateTime;

@Configuration
public class NomMasteredRessursKafkaConfig {

    @Bean
    ProducerFactory<String, LocalDateTime> nomMasteredRessursProducerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        final var jsonSerializer = new JacksonJsonSerializer<LocalDateTime>(jsonMapper);
        properties.setClientId("nomMasteredRessursProducer");
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), jsonSerializer);
    }

    @Bean
    KafkaTemplate<String, LocalDateTime> nomMasteredRessursProducerTemplate(
        ProducerFactory<String, LocalDateTime> nomMasteredRessursProducerFactory,
        @Value("${app.kafka.ressurs.nomMasteredRessursTopic}") String nomMasteredRessursTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(nomMasteredRessursProducerFactory);
        eventsourceTemplate.setDefaultTopic(nomMasteredRessursTopic);

        return eventsourceTemplate;
    }
}
