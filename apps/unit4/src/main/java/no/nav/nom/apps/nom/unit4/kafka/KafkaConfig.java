package no.nav.nom.apps.nom.unit4.kafka;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@EnableKafka
@Configuration
public class KafkaConfig {

    /* Ressurs Topic */

    @Bean
    public ConsumerFactory<String, NomRessursDto> consumerFactory(KafkaProperties properties) {
        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, NomRessursDto>>
            kafkaListenerContainerFactory(ConsumerFactory<String, NomRessursDto> consumerFactory) {
        var concurrentKafkaListenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, NomRessursDto>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(consumerFactory);

        return concurrentKafkaListenerContainerFactory;
    }


}

