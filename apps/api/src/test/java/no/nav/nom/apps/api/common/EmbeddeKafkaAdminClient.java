package no.nav.nom.apps.api.common;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

public class EmbeddeKafkaAdminClient {
    private final EmbeddedKafkaBroker embeddedKafkaBroker;

    public EmbeddeKafkaAdminClient(EmbeddedKafkaBroker embeddedKafkaBroker) {
        this.embeddedKafkaBroker = embeddedKafkaBroker;
    }

    @Bean
    public Admin adminClient() {
        return AdminClient.create(KafkaTestUtils.producerProps(embeddedKafkaBroker));
    }
}

