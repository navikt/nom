package no.nav.nom.libs.state.orgenhetleder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import static no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateAutoconfiguration.getDefaultOrgEnhetLederConsumerFactory;

@Configuration
public class KafkaTestFactoryConfiguration {

    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    ObjectMapper objectMapper = configureObjectMapper();

    @Bean
    ConsumerFactory<String, OrgEnhetLederState> nomStateOrgEnhetLederConsumerFactory() {
        return getDefaultOrgEnhetLederConsumerFactory(KafkaTestUtils.producerProps(embeddedKafka), objectMapper);
    }

    @Bean
    ProducerFactory<String, OrgEnhetLederState> orgEnhetLederStateProducerFactory() {
        return new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(embeddedKafka), new StringSerializer(), new JsonSerializer<>(objectMapper));
    }

    @Bean
    KafkaTemplate<String, OrgEnhetLederState> orgEnhetLederStateProducerTemplate(
        ProducerFactory<String, OrgEnhetLederState> orgEnhetLederStateProducerFactory, @Value("${nom.libs.state.orgenhet-leder.state-topic}") String eventsourceTopic
    ) {
        final var eventsourceTemplate = new KafkaTemplate<>(orgEnhetLederStateProducerFactory);
        eventsourceTemplate.setDefaultTopic(eventsourceTopic);

        return eventsourceTemplate;
    }

    @Bean
    KafkaTemplate<String, String> stringProducerTemplate() {
        ProducerFactory<String, String> stringStateProducerFactory =
            new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(embeddedKafka), new StringSerializer(), new StringSerializer());
        return new KafkaTemplate<>(stringStateProducerFactory);
    }

    private static ObjectMapper configureObjectMapper() {
        final var objectMapper = new ObjectMapper();

        return objectMapper.findAndRegisterModules()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
}
