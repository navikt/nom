package no.nav.nom.apps.api.ressurs.masterdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Duration;
import java.time.LocalDateTime;

@Configuration
public class NomMasteredRessursKafkaConfig {

    @Bean
    ProducerFactory<String, LocalDateTime> nomMasteredRessursProducerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        final var jsonSerializer = new JsonSerializer<LocalDateTime>(objectMapper);
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
