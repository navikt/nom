package no.nav.nom.apps.nom.unit4.kafka.utils;


import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class TestKafkaConfig {

    /* Ressurs Topic */

    @Bean
    public ProducerFactory<String, RessursEvent> producerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        JsonSerializer<RessursEvent> eventJsonSerializer = new JsonSerializer<>(objectMapper);
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), eventJsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, RessursEvent> kafkaTemplate(ProducerFactory<String, RessursEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }


}

