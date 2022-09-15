package no.nav.nom.apps.nom.unit4.kafka.utils;

import no.nav.nom.apps.nom.unit4.kafka.KafkaRessursDto;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;

@Configuration
public class TestKafkaConfig {

    /* Ressurs Topic */

    @Bean
    public ProducerFactory<String, KafkaRessursDto> producerFactory(KafkaProperties properties) {
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties());
    }

    @Bean
    public KafkaTemplate<String, KafkaRessursDto> kafkaTemplate(ProducerFactory<String, KafkaRessursDto> producerFactory) {
        KafkaTemplate<String, KafkaRessursDto> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        return kafkaTemplate;
    }


}

