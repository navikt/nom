package no.nav.nom.apps.nom.unit4.kafka.utils;


import tools.jackson.databind.json.JsonMapper;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

@Configuration
public class TestKafkaConfig {

    /* Ressurs Topic */

    @Bean
    public ProducerFactory<String, RessursEvent> producerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        JacksonJsonSerializer<RessursEvent> eventJsonSerializer = new JacksonJsonSerializer<>(jsonMapper);
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), eventJsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, RessursEvent> kafkaTemplate(ProducerFactory<String, RessursEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }


}

