package no.nav.nom.apps.api.common;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaAdminClient {
    private final KafkaAdmin kafkaAdmin;

    public KafkaAdminClient(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Bean
    public Admin adminClient() {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }
}
