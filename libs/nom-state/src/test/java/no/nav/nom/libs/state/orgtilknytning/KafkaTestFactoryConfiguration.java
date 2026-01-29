package no.nav.nom.libs.state.orgtilknytning;

import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import static no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateAutoconfiguration.getDefaultOrgtilknytningConsumerFactory;

@Configuration
public class KafkaTestFactoryConfiguration {

    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    JsonMapper jsonMapper = configureJsonMapper();

    @Bean
    ConsumerFactory<String, OrgtilknytningState> nomStateOrgtilknytningConsumerFactory() {
        return getDefaultOrgtilknytningConsumerFactory(KafkaTestUtils.producerProps(embeddedKafka), jsonMapper);
    }

    @Bean
    ProducerFactory<String, OrgtilknytningState> orgtilknytningStateProducerFactory() {
        return new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(embeddedKafka), new StringSerializer(), new JacksonJsonSerializer<>(jsonMapper));
    }

    @Bean
    KafkaTemplate<String, OrgtilknytningState> orgtilknytningStateProducerTemplate(
        ProducerFactory<String, OrgtilknytningState> orgtilknytningStateProducerFactory, @Value("${nom.libs.state.orgtilknytning.state-topic}") String eventsourceTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(orgtilknytningStateProducerFactory);
        eventsourceTemplate.setDefaultTopic(eventsourceTopic);

        return eventsourceTemplate;
    }

    @Bean
    KafkaTemplate<String, String> stringProducerTemplate() {
        ProducerFactory<String, String> stringStateProducerFactory =
            new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(embeddedKafka), new StringSerializer(), new StringSerializer());
        return new KafkaTemplate<>(stringStateProducerFactory);
    }

    private static JsonMapper configureJsonMapper() {
        final var jsonMapper = new JsonMapper();

        return jsonMapper.rebuild()
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();
    }
}
