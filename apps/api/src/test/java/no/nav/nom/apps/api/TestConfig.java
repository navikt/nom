package no.nav.nom.apps.api;

import no.nav.nom.libs.test.SecurityTestUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

@SpringBootApplication(exclude = KafkaAutoConfiguration.class)
@Import({SecurityTestUtils.class, ApiStarter.class})
class TestConfig {

    @Bean
    KafkaTemplate<String, String> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }
}
