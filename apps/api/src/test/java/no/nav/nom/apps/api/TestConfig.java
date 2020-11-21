package no.nav.nom.apps.api;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = KafkaAutoConfiguration.class)
@Import(ApiStarter.class)
class TestConfig {
}
