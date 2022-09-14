package no.nav.nom.apps.nom.unit4.kafka.utils;

import no.nav.nom.apps.nom.unit4.kafka.NomRessursDto;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.*;

@Configuration
public class TestKafkaConfig {

    /* Ressurs Topic */

    @Bean
    public ProducerFactory<String, NomRessursDto> producerFactory(KafkaProperties properties) {
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties());
    }

    @Bean
    public KafkaTemplate<String, NomRessursDto> kafkaTemplate(ProducerFactory<String, NomRessursDto> producerFactory) {
        KafkaTemplate<String, NomRessursDto> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        return kafkaTemplate;
    }


}

