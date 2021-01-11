package no.nav.nom.apps.dvh;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

@SpringBootApplication(exclude = KafkaAutoConfiguration.class)
@Import(DvhStarter.class)
public class TestConfig {

    @Bean
    ConsumerFactory<String, String> consumerFactory() {
        return mock(ConsumerFactory.class);
    }

    @Bean
    KafkaTemplate<String, String> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }
}
