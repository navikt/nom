package no.nav.nom.apps.nom.unit4.kafka;

import tools.jackson.databind.json.JsonMapper;
import no.nav.nom.apps.nom.unit4.AppProperties;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    /* Ressurs Topic */

    @Bean
    public ConsumerFactory<String, RessursEvent> consumerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        JacksonJsonDeserializer<RessursEvent> jsonEventDeserializer = new JacksonJsonDeserializer<>(jsonMapper);
        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(), new StringDeserializer(), jsonEventDeserializer);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, RessursEvent>>
            kafkaListenerContainerFactory(ConsumerFactory<String, RessursEvent> consumerFactory, DefaultErrorHandler errorHandler) {
        var concurrentKafkaListenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, RessursEvent>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(consumerFactory);
        concurrentKafkaListenerContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        concurrentKafkaListenerContainerFactory.setCommonErrorHandler(errorHandler);

        return concurrentKafkaListenerContainerFactory;
    }

    @Bean
    public DefaultErrorHandler errorHandler(AppProperties appProperties) {
        var interval = appProperties.kafka().listener().blockSecondsOnBail().toMillis();

        BackOff fixedBackOff = new FixedBackOff(interval, FixedBackOff.UNLIMITED_ATTEMPTS);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(fixedBackOff);

        errorHandler.addRetryableExceptions(KafkaRessursBlockingException.class);

        return errorHandler;
    }

    @Bean
    public ProducerFactory<String, RessursEvent> ressursEventDltProducerFactory(KafkaProperties properties) {
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties());
    }

    @Bean
    public KafkaTemplate<String, RessursEvent> ressursEventDltKafkaTemplate(ProducerFactory<String, RessursEvent> producerFactory) {
        KafkaTemplate<String, RessursEvent> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        return kafkaTemplate;
    }
}

