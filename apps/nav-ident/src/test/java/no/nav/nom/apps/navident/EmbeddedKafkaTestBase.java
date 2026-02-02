package no.nav.nom.apps.navident;

import org.springframework.kafka.test.context.EmbeddedKafka;

@EmbeddedKafka(
        partitions = 1,
        topics = {"${test.topic}", "${test.rnr-topic}"}
)
public abstract class EmbeddedKafkaTestBase extends SpringBootTestBase {
}
