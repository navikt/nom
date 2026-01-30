package no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer;

import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederEvent;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class OrgEnhetLederEventKafkaConfig {

    @Bean
    ProducerFactory<String, OrgEnhetLederEvent> orgEnhetLederEventProducerFactory(KafkaProperties properties, JsonMapper jsonMapper) {
        final var jsonSerializer = new JacksonJsonSerializer<OrgEnhetLederEvent>(jsonMapper);
        properties.setClientId("orgEnhetLederEventProducer");
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), jsonSerializer);
    }

    @Bean
    KafkaTemplate<String, OrgEnhetLederEvent> orgEnhetLederEventProducerTemplate(
        ProducerFactory<String, OrgEnhetLederEvent> orgEnhetLederEventProducerFactory, @Value("${app.kafka.orgEnhetLeder.eventsource-topic}") String eventsourceTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(orgEnhetLederEventProducerFactory);
        eventsourceTemplate.setDefaultTopic(eventsourceTopic);
        return eventsourceTemplate;
    }
}
