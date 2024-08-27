package no.nav.nom.apps.admin.events.orgtilknytning.eventsource;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.Duration;

@Slf4j
@Configuration
@EnableKafka
public class OrgtilknytningEvtsrcKafkaConfig {
    @Bean
    ConsumerFactory<String, OrgtilknytningEvent> orgtilknytningEvtsrcConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(
            properties.buildConsumerProperties(),
            new StringDeserializer(),
            new JsonDeserializer<>(OrgtilknytningEvent.class, objectMapper)
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, OrgtilknytningEvent> orgtilknytningEvtsrcKafkaListenerContainerFactory(
        ConsumerFactory<String, OrgtilknytningEvent> consumerFactory
    ) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, OrgtilknytningEvent>();
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
