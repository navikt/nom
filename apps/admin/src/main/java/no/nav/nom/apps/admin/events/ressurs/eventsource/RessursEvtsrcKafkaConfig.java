package no.nav.nom.apps.admin.events.ressurs.eventsource;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@Slf4j
@Configuration
@EnableKafka
public class RessursEvtsrcKafkaConfig {

    @Bean
    ConsumerFactory<String, RessursEvent> ressursEvtsrcConsumerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        return new DefaultKafkaConsumerFactory<>(
            properties.buildConsumerProperties(),
            new StringDeserializer(),
            new JacksonJsonDeserializer<>(RessursEvent.class, jsonMapper)
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, RessursEvent> ressursEvtsrcKafkaListenerContainerFactory(
        ConsumerFactory<String, RessursEvent> consumerFactory
    ) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, RessursEvent>();
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
