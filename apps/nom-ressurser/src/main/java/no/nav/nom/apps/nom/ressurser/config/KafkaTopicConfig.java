package no.nav.nom.apps.nom.ressurser.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("local")
public class KafkaTopicConfig {

    @Value("${kafka.bootstrapAddress}")
    private String bootstrapAddress;

    @Value("${kafka.topic.ressurs.name}")
    private String ressursTopicName;

    @Value("${kafka.topic.ressurs.partitions}")
    private int ressursNumPartitions;

    @Value("${kafka.topic.ressurs.replicas}")
    private short ressursNumReplicas;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic ressursTopic() {
        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT);
        configs.put(TopicConfig.RETENTION_MS_CONFIG, "-1");

        NewTopic ressursTopic = new NewTopic(ressursTopicName, ressursNumPartitions, ressursNumReplicas);
        ressursTopic.configs(configs);

        return ressursTopic;
    }
}
