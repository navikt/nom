package no.nav.nom.apps.nomremedyintegrasjon;

import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class TestConfiguration {

    @Primary
    @Bean(name = "nomRemedyIntegrasjonDataSource")
    @Profile("test")
    public DataSource dataSource(
        @Qualifier("nomRemedyIntegrasjonDataSourceProperties") DataSourceProperties dataSourceProperties
    ) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean
    public ProducerFactory<String, RessursEvent> ressursEventProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> configProps = new HashMap<>(kafkaProperties.buildProducerProperties());
        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(), new JacksonJsonSerializer<>());
    }

    @Bean
    public KafkaTemplate<String, RessursEvent> ressursEventKafkaTemplate(
        ProducerFactory<String, RessursEvent> ressursEventProducerFactory
    ) {
        return new KafkaTemplate<>(ressursEventProducerFactory);
    }

    @Bean
    public ProducerFactory<String, OrgtilknytningEvent> orgtilknytningEventProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> configProps = new HashMap<>(kafkaProperties.buildProducerProperties());
        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(), new JacksonJsonSerializer<>());
    }

    @Bean
    public KafkaTemplate<String, OrgtilknytningEvent> orgtilknytningEventKafkaTemplate(
        ProducerFactory<String, OrgtilknytningEvent> orgtilknytningEventProducerFactory
    ) {
        return new KafkaTemplate<>(orgtilknytningEventProducerFactory);
    }

}
