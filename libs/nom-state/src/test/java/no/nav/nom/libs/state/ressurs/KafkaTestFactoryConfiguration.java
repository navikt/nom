package no.nav.nom.libs.state.ressurs;

import no.nav.nom.contract.nom.state.RessursState;
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

import static no.nav.nom.libs.state.ressurs.RessursStateAutoconfiguration.getDefaultRessursConsumerFactory;

@Configuration
public class KafkaTestFactoryConfiguration {

    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    JsonMapper jsonMapper = configureJsonMapper();

    @Bean
    ConsumerFactory<String, RessursState> nomStateRessursConsumerFactory() {
        return getDefaultRessursConsumerFactory(KafkaTestUtils.producerProps(embeddedKafka), jsonMapper);
    }

    @Bean
    ProducerFactory<String, RessursState> ressursStateProducerFactory() {
        return new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(embeddedKafka), new StringSerializer(), new JacksonJsonSerializer<>(jsonMapper));
    }

    @Bean
    KafkaTemplate<String, RessursState> ressursStateProducerTemplate(
        ProducerFactory<String, RessursState> ressursStateProducerFactory, @Value("${nom.libs.state.ressurs.state-topic}") String eventsourceTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(ressursStateProducerFactory);
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
