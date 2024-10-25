package no.nav.nom.apps.api.orgtilknytning.dvh;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class GmlDvhLastResultKafkaConfig {

    @Bean
    ProducerFactory<String, StrukturerteStillingPerioderForNavident> gmlDvhLastResultProducerFactory(KafkaProperties properties, ObjectMapper objectMapper) {
        final var jsonSerializer = new JsonSerializer<StrukturerteStillingPerioderForNavident>(objectMapper);
        properties.setClientId("gmlDvhLastResultProducer");
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), jsonSerializer);
    }

    @Bean
    KafkaTemplate<String, StrukturerteStillingPerioderForNavident> gmlDvhLastResultProducerTemplate(
        ProducerFactory<String, StrukturerteStillingPerioderForNavident> dvhLastResultProducerFactory,
        @Value("${app.kafka.orgtilknytning.gmlDvhLastResultTopic}") String dvhLastResultTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(dvhLastResultProducerFactory);
        eventsourceTemplate.setDefaultTopic(dvhLastResultTopic);

        return eventsourceTemplate;
    }
}
