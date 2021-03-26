package no.nav.nom.apps.api.common;

import no.nav.nom.libs.utils.common.kafka.KafkaContainerUtils;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

import static no.nav.nom.libs.utils.common.kafka.KafkaContainerUtils.DEFAULT_SPRING_KAFKA_FACTORY_NAME;


@EnableKafka
@Profile("kafka")
@Configuration(proxyBeanMethods = false)
public class KafkaConfig {

    @SuppressWarnings("unchecked")
    @Bean(DEFAULT_SPRING_KAFKA_FACTORY_NAME) // Overskriver default Spring Boot, se KafkaAnnotationDrivenConfiguration
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            KafkaOffsetPostProcessor offsetPostProcessor) {
        var factory = KafkaContainerUtils.containerFactory(configurer, kafkaConsumerFactory);
        ((ConsumerFactory<Object, Object>) factory.getConsumerFactory()).addPostProcessor(offsetPostProcessor);
        return factory;
    }

}
