package no.nav.nom.libs.utils.common.kafka;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

@UtilityClass
public class KafkaContainerUtils {
    public static final String DEFAULT_SPRING_KAFKA_FACTORY_NAME = "kafkaListenerContainerFactory";

    public static ConcurrentKafkaListenerContainerFactory<?, ?> containerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory) {

        val factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.getContainerProperties().setSubBatchPerPartition(false);
        return factory;
    }
}
