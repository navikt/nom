package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import no.nav.nom.generated.model.OrgtilknytningState;
import no.nav.nom.generated.model.RessursState;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
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
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, no.nav.nom.contract.nom.state.RessursState>>
    nomRessursStateKafkaListenerContainerFactory(ConsumerFactory<String, no.nav.nom.contract.nom.state.RessursState> nomRessursStateConsumerFactory,
                                                 DefaultErrorHandler ressursErrorHandler) {
        var concurrentKafkaListenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, no.nav.nom.contract.nom.state.RessursState>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(nomRessursStateConsumerFactory);
        concurrentKafkaListenerContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        concurrentKafkaListenerContainerFactory.setCommonErrorHandler(ressursErrorHandler);

        return concurrentKafkaListenerContainerFactory;
    }

    @Bean
    public ConsumerFactory<String, RessursState> nomRessursStateConsumerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        JacksonJsonDeserializer<RessursState> jacksonJsonDeserializer = new JacksonJsonDeserializer<>(RessursState.class, jsonMapper);
        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(), new StringDeserializer(), jacksonJsonDeserializer);
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
    public ConsumerFactory<String, OrgtilknytningState> nomOrgtilknytningStateConsumerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        JacksonJsonDeserializer<OrgtilknytningState> jacksonJsonDeserializer = new JacksonJsonDeserializer<>(OrgtilknytningState.class, jsonMapper);
        return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties(), new StringDeserializer(), jacksonJsonDeserializer);
    }

    @Bean
    protected DefaultErrorHandler ressursErrorHandler(
        KafkaTemplate<String, no.nav.nom.contract.nom.state.RessursState> kafkaTemplate,
        @Value("${app.kafka.ressurs.dlt-topic}") String dltTopic
    ) {
        DeadLetterPublishingRecoverer deadLetterPublishingRecoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, (record, ex) -> new TopicPartition(dltTopic, record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetterPublishingRecoverer, new FixedBackOff(0L, 0L));
        errorHandler.addNotRetryableExceptions(DeserializationException.class);
        errorHandler.addNotRetryableExceptions(ConverterNotFoundException.class);
        errorHandler.addNotRetryableExceptions(MessageConversionException.class);
        errorHandler.addNotRetryableExceptions(SerializationException.class);
        return errorHandler;
    }

    @Bean
    protected DefaultErrorHandler orgtilknytningErrorHandler(
        KafkaTemplate<String, no.nav.nom.contract.nom.state.OrgtilknytningState> kafkaTemplate,
        @Value("${app.kafka.orgtilknytning.dlt-topic}") String dltTopic
    ) {
        DeadLetterPublishingRecoverer deadLetterPublishingRecoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, (record, ex) -> new TopicPartition(dltTopic, record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetterPublishingRecoverer, new FixedBackOff(1000L, 3L));
        errorHandler.addNotRetryableExceptions(DeserializationException.class);
        errorHandler.addNotRetryableExceptions(ConverterNotFoundException.class);
        errorHandler.addNotRetryableExceptions(MessageConversionException.class);
        errorHandler.addNotRetryableExceptions(SerializationException.class);
        return errorHandler;
    }

    @Bean
    ProducerFactory<String, no.nav.nom.contract.nom.state.OrgtilknytningState> nomOrgtilknytningStateDltProducerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        final var jsonSerializer = new JacksonJsonSerializer<no.nav.nom.contract.nom.state.OrgtilknytningState>(jsonMapper);
        properties.setClientId("nomOrgtilknytningStateDltProducer");
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), jsonSerializer);
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
    ProducerFactory<String, no.nav.nom.contract.nom.state.RessursState> nomRessursStateDltProducerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        final var jsonSerializer = new JacksonJsonSerializer<no.nav.nom.contract.nom.state.RessursState>(jsonMapper);
        properties.setClientId("nomRessursStateDltProducer");
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), jsonSerializer);
    }

    @Bean
    KafkaTemplate<String, no.nav.nom.contract.nom.state.RessursState> nomRessursStateProducerTemplate(
        ProducerFactory<String, no.nav.nom.contract.nom.state.RessursState> nomRessursStateDltProducerFactory,
        @Value("${app.kafka.ressurs.dlt-topic}") String dltTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(nomRessursStateDltProducerFactory);
        eventsourceTemplate.setDefaultTopic(dltTopic);
        return eventsourceTemplate;
    }
}
