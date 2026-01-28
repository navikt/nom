package no.nav.nom.apps.api.topicreader;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class KafkaTopicReader {

    @Value("${app.kafka.ressurs.eventsourceTopic}") String ressursEventSourcetopic;
    @Value("${app.kafka.orgEnhet.eventsourceTopic}") String orgEnhetEventSourcetopic;
    private final KafkaProperties kafkaProperties;
    private final JsonMapper jsonMapper;

    public KafkaTopicReader(KafkaProperties kafkaProperties, JsonMapper jsonMapper) {
        this.kafkaProperties = kafkaProperties;
        this.jsonMapper = jsonMapper;
    }

    public List<JsonNode> getRessursEvents(int maxRecords, long fromOffset, int partition) {
        return getLatestRecordFromTopic(ressursEventSourcetopic, maxRecords, fromOffset, partition);
    }

    public List<JsonNode> getOrgEnhetEvents(int maxRecords, long fromOffset, int partition) {
        return getLatestRecordFromTopic(orgEnhetEventSourcetopic,  maxRecords, fromOffset, partition);

    }

    private List<JsonNode> getLatestRecordFromTopic(String topic, int maxRecords, long fromOffset, int partition) {
        try (Consumer<String, String> kafkaConsumer = new KafkaConsumer<>(kafkaProperties.buildConsumerProperties())) {
            List<JsonNode> kafkaRecords = new ArrayList<>();
            TopicPartition topicPartition = new TopicPartition(topic, partition);

            kafkaConsumer.assign(List.of(topicPartition));

            kafkaConsumer.seekToEnd(List.of(topicPartition));
            long endPosition = kafkaConsumer.position(topicPartition);
            long positionToReadFrom = endPosition - fromOffset;

            kafkaConsumer.seek(topicPartition, positionToReadFrom);

            while (kafkaRecords.size() < maxRecords) {
                ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(100));

                if (consumerRecords.isEmpty()) {
                    break;
                }

                consumerRecords.records(topicPartition).forEach(record -> {
                    consumerRecordToJsonNode(record).ifPresent(kafkaRecords::add);
                });

            }

            if (kafkaRecords.size() > maxRecords) {
                return kafkaRecords.subList(0, maxRecords);
            }

            return kafkaRecords;
        }
    }

    private Optional<JsonNode> consumerRecordToJsonNode(ConsumerRecord<String, String> record) {
        try {
            return Optional.of(jsonMapper.readTree(record.value()));
        } catch (JacksonException ignored) {
        }
        return Optional.empty();
    }
}
