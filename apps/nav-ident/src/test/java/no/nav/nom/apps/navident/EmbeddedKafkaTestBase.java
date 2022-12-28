package no.nav.nom.apps.navident;

import org.springframework.kafka.test.context.EmbeddedKafka;

@EmbeddedKafka(
        partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
        topics = {"${test.topic}", "${test.rnr-topic}"}
)
public abstract class EmbeddedKafkaTestBase extends SpringBootTestBase {
}
