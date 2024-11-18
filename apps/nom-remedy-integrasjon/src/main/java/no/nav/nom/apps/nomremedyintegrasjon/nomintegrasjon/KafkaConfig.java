package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.LocalDateTime;

@Configuration
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, RessursEvent> ressursEventConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        JsonDeserializer<RessursEvent> jsonEventDeserializer = new JsonDeserializer<>(objectMapper);
        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(), new StringDeserializer(), jsonEventDeserializer);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, RessursEvent>>
    ressursEventKafkaListenerContainerFactory(ConsumerFactory<String, RessursEvent> ressursEventConsumerFactory) {
        var concurrentKafkaListenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, RessursEvent>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(ressursEventConsumerFactory);
        concurrentKafkaListenerContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return concurrentKafkaListenerContainerFactory;
    }

    @Bean
    public ConsumerFactory<String, OrgtilknytningEvent> orgtilknytningEventConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        JsonDeserializer<OrgtilknytningEvent> jsonEventDeserializer = new JsonDeserializer<>(objectMapper);
        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(), new StringDeserializer(), jsonEventDeserializer);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, OrgtilknytningEvent>>
    orgtilknytningEventKafkaListenerContainerFactory(ConsumerFactory<String, OrgtilknytningEvent> orgtilknytningEventConsumerFactory) {
        var concurrentKafkaListenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, OrgtilknytningEvent>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(orgtilknytningEventConsumerFactory);
        concurrentKafkaListenerContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return concurrentKafkaListenerContainerFactory;
    }

    @Bean
    public ConsumerFactory<String, LocalDateTime> nomMasteredRessursEventConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        JsonDeserializer<LocalDateTime> jsonEventDeserializer = new JsonDeserializer<>(objectMapper);
        jsonEventDeserializer.addTrustedPackages("java.time.LocalDateTime");
        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(), new StringDeserializer(), jsonEventDeserializer);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, LocalDateTime>>
    nomMasteredRessursEventKafkaListenerContainerFactory(ConsumerFactory<String, LocalDateTime> nomMasteredRessursEventConsumerFactory) {
        var concurrentKafkaListenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, LocalDateTime>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(nomMasteredRessursEventConsumerFactory);
        concurrentKafkaListenerContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return concurrentKafkaListenerContainerFactory;
    }
}
