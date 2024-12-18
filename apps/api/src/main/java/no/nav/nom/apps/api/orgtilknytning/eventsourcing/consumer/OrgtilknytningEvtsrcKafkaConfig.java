package no.nav.nom.apps.api.orgtilknytning.eventsourcing.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.time.Duration;

@Slf4j
@Configuration
public class OrgtilknytningEvtsrcKafkaConfig {
    @Bean
    ConsumerFactory<String, String> orgtilknytningEvtsrcConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(
            properties.buildConsumerProperties(null),
            new StringDeserializer(),
            new StringDeserializer()
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> orgtilknytningEvtsrcKafkaListenerContainerFactory(
        ConsumerFactory<String, String> consumerFactory
    ) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setIdleEventInterval(5 * 1000L);

        // If authentication towards kafka broker topic fails, retry every 10s
        factory.setContainerCustomizer(container -> {
                container.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10));
                log.info(container.toString());
            }
        );
        return factory;
    }
}
