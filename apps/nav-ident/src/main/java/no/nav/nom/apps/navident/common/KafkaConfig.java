package no.nav.nom.apps.navident.common;

import no.nav.nom.apps.navident.eventsource.SourceEvent;
import no.nav.nom.apps.navident.rnrservices.Need;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@EnableKafka
@Configuration
public class KafkaConfig {

    /* EventSource Topic */

    @Bean
    public ConsumerFactory<String, SourceEvent> consumerFactory(KafkaProperties properties) {
        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, SourceEvent>>
            kafkaListenerContainerFactory(ConsumerFactory<String, SourceEvent> consumerFactory,
                                          @Value("${app.kafka.eventSourceIdleTimeout}") Long idleTimeout) {
        var concurrentKafkaListenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, SourceEvent>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(consumerFactory);

        concurrentKafkaListenerContainerFactory.getContainerProperties().setIdleEventInterval(idleTimeout);

        return concurrentKafkaListenerContainerFactory;
    }

    @Bean
    public ProducerFactory<String, SourceEvent> producerFactory(KafkaProperties properties) {
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties());
    }

    @Bean
    public KafkaTemplate<String, SourceEvent> kafkaTemplate(ProducerFactory<String, SourceEvent> producerFactory) {
        KafkaTemplate<String, SourceEvent> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        return kafkaTemplate;
    }

    /* Need Topic */

    @Bean
    public ConsumerFactory<String, Need> needConsumerFactory(KafkaProperties properties) {
        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Need>> needKafkaListenerContainerFactory(ConsumerFactory<String, Need> consumerFactory) {
        var concurrentKafkaListenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, Need>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(consumerFactory);

        return concurrentKafkaListenerContainerFactory;
    }

    @Bean
    public ProducerFactory<String, Need> needProducerFactory(KafkaProperties properties) {
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties());
    }

    @Bean
    public KafkaTemplate<String, Need> needKafkaTemplate(ProducerFactory<String, Need> producerFactory) {
        KafkaTemplate<String, Need> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        return kafkaTemplate;
    }
}

