package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.LocalDateTime;

@Configuration
public class DvhLastResultKafkaConfig {

    @Bean
    ProducerFactory<String, StrukturerteStillingPerioderForNavident> dvhLastResultProducerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        final var jsonSerializer = new JsonSerializer<StrukturerteStillingPerioderForNavident>(objectMapper);
        properties.setClientId("dvhLastResultProducer");
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), jsonSerializer);
    }

    @Bean
    KafkaTemplate<String, StrukturerteStillingPerioderForNavident> dvhLastResultProducerTemplate(
        ProducerFactory<String, StrukturerteStillingPerioderForNavident> dvhLastResultProducerFactory,
        @Value("${app.kafka.orgtilknytning.dvhLastResultTopic}") String dvhLastResultTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(dvhLastResultProducerFactory);
        eventsourceTemplate.setDefaultTopic(dvhLastResultTopic);

        return eventsourceTemplate;
    }
}
