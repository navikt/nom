package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.generated.model.OrgtilknytningState;
import no.nav.nom.generated.model.RessursState;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.backoff.FixedBackOff;

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

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, no.nav.nom.contract.nom.state.RessursState>>
    nomRessursStateKafkaListenerContainerFactory(ConsumerFactory<String, no.nav.nom.contract.nom.state.RessursState> nomRessursStateConsumerFactory, DefaultErrorHandler ressursErrorHandler) {
        var concurrentKafkaListenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, no.nav.nom.contract.nom.state.RessursState>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(nomRessursStateConsumerFactory);
        concurrentKafkaListenerContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        concurrentKafkaListenerContainerFactory.setCommonErrorHandler(ressursErrorHandler);

        return concurrentKafkaListenerContainerFactory;
    }

    @Bean
    public ConsumerFactory<String, RessursState> nomRessursStateConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        JsonDeserializer<RessursState> jsonDeserializer = new JsonDeserializer<>(RessursState.class, objectMapper);
        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(), new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, no.nav.nom.contract.nom.state.OrgtilknytningState>>
    nomOrgtilknytningStateKafkaListenerContainerFactory(ConsumerFactory<String, no.nav.nom.contract.nom.state.OrgtilknytningState> nomOrgtilknytningStateConsumerFactory, DefaultErrorHandler orgtilknytningErrorHandler) {
        var concurrentKafkaListenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, no.nav.nom.contract.nom.state.OrgtilknytningState>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(nomOrgtilknytningStateConsumerFactory);
        concurrentKafkaListenerContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        concurrentKafkaListenerContainerFactory.setCommonErrorHandler(orgtilknytningErrorHandler);

        return concurrentKafkaListenerContainerFactory;
    }

    @Bean
    public ConsumerFactory<String, OrgtilknytningState> nomOrgtilknytningStateConsumerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        JsonDeserializer<OrgtilknytningState> jsonDeserializer = new JsonDeserializer<>(OrgtilknytningState.class, objectMapper);
        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(), new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    protected DefaultErrorHandler ressursErrorHandler(
        KafkaTemplate<String, no.nav.nom.contract.nom.state.RessursState> kafkaTemplate
    ) {
        DeadLetterPublishingRecoverer deadLetterPublishingRecoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, (record, ex) -> new TopicPartition(record.topic() + "-DLT", record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetterPublishingRecoverer, new FixedBackOff(0L, 0L));
        errorHandler.addNotRetryableExceptions(DeserializationException.class);
        errorHandler.addNotRetryableExceptions(ConverterNotFoundException.class);
        errorHandler.addNotRetryableExceptions(MessageConversionException.class);
        errorHandler.addNotRetryableExceptions(SerializationException.class);
        return errorHandler;
    }

    @Bean
    protected DefaultErrorHandler orgtilknytningErrorHandler(
        KafkaTemplate<String, no.nav.nom.contract.nom.state.OrgtilknytningState> kafkaTemplate
    ) {
        DeadLetterPublishingRecoverer deadLetterPublishingRecoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, (record, ex) -> new TopicPartition(record.topic() + "-DLT", record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetterPublishingRecoverer, new FixedBackOff(1000L, 3L));
        errorHandler.addNotRetryableExceptions(DeserializationException.class);
        errorHandler.addNotRetryableExceptions(ConverterNotFoundException.class);
        errorHandler.addNotRetryableExceptions(MessageConversionException.class);
        errorHandler.addNotRetryableExceptions(SerializationException.class);
        return errorHandler;
    }

    @Bean
    ProducerFactory<String, no.nav.nom.contract.nom.state.OrgtilknytningState> nomOrgtilknytningStateDltProducerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        final var jsonSerializer = new JsonSerializer<no.nav.nom.contract.nom.state.OrgtilknytningState>(objectMapper);
        properties.setClientId("nomOrgtilknytningStateDltProducer");
        return new DefaultKafkaProducerFactory<>(properties.buildConsumerProperties(), new StringSerializer(), jsonSerializer);
    }

    @Bean
    KafkaTemplate<String, no.nav.nom.contract.nom.state.OrgtilknytningState> nomOrgtilknytningStateProducerTemplate(
        ProducerFactory<String, no.nav.nom.contract.nom.state.OrgtilknytningState> nomOrgtilknytningStateDltProducerFactory,
        @Value("${app.kafka.orgtilknytning.dlt-topic}") String dltTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(nomOrgtilknytningStateDltProducerFactory);
        eventsourceTemplate.setDefaultTopic(dltTopic);
        return eventsourceTemplate;
    }

    @Bean
    ProducerFactory<String, no.nav.nom.contract.nom.state.RessursState> nomRessursStateDltProducerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        final var jsonSerializer = new JsonSerializer<no.nav.nom.contract.nom.state.RessursState>(objectMapper);
        properties.setClientId("nomRessursStateDltProducer");
        return new DefaultKafkaProducerFactory<>(properties.buildConsumerProperties(), new StringSerializer(), jsonSerializer);
    }

    @Bean
    KafkaTemplate<String, no.nav.nom.contract.nom.state.RessursState> nomRessursStateProducerTemplate(
        ProducerFactory<String, no.nav.nom.contract.nom.state.RessursState> nomRessursStateDltProducerFactory,
        @Value("${app.kafka.orgtilknytning.dlt-topic}") String dltTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(nomRessursStateDltProducerFactory);
        eventsourceTemplate.setDefaultTopic(dltTopic);
        return eventsourceTemplate;
    }

}
