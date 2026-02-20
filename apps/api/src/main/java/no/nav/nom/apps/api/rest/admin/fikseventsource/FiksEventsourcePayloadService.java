package no.nav.nom.apps.api.rest.admin.fikseventsource;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FiksEventsourcePayloadService {

    private final KafkaProperties kafkaProperties;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.kafka.ressurs.eventsourceTopic}") String ressursEventsourceTopic;
    @Value("${app.kafka.orgEnhet.eventsourceTopic}") String orgenhetEventsourceTopic;
    @Value("${app.kafka.orgEnhetLeder.eventsource-topic}") String orgenhetLederEventsourceTopic;
    @Value("${app.kafka.orgtilknytning.eventsource-topic}") String orgtilknytningEventsourceTopic;
    @Value("${app.kafka.organisering.eventsourceTopic}") String organiseringEventsourceTopic;
    @Value("${app.kafka.authorize.eventsource-topic}") String authorizeEventsourceTopic;

    @Value("${app.fiksEventsourcePayload.offsets.ressurs:0}") long ressursOffset;
    @Value("${app.fiksEventsourcePayload.offsets.orgenhet:0}") long orgenhetOffset;
    @Value("${app.fiksEventsourcePayload.offsets.orgenhetLeder:0}") long orgenhetLederOffset;
    @Value("${app.fiksEventsourcePayload.offsets.orgtilknytning:0}") long orgtilknytningOffset;
    @Value("${app.fiksEventsourcePayload.offsets.organisering:0}") long organiseringOffset;
    @Value("${app.fiksEventsourcePayload.offsets.authorize:0}") long authorizeOffset;

    public FiksEventsourcePayloadService(KafkaProperties kafkaProperties, JdbcTemplate jdbcTemplate) {
        this.kafkaProperties = kafkaProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public String repair(EventSourceTable table, boolean dryRun) {
        StringBuilder output = new StringBuilder();
        String tableName = table.getTableName();
        String topic = resolveTopicName(table);
        long fromOffset = resolveOffset(table);

        output.append("=== Reparering av %s ===\n".formatted(tableName));
        output.append("Kafka topic: %s\n".formatted(topic));
        output.append("Leser fra offset: %s\n".formatted(fromOffset));
        output.append("Dry run: %s\n\n".formatted(dryRun));

        // Step 1: Read all messages from Kafka topic into a HashMap<eventKey, payloadJson>
        output.append("Leser meldinger fra Kafka topic...\n");
        Map<String, String> kafkaMessages = readKafkaTopicFromOffset(topic, fromOffset);
        output.append("Lest %s meldinger fra Kafka topic\n\n".formatted(kafkaMessages.size()));

        // Step 2: Find all erroneous records in the DB
        String findErroneousSql = "SELECT event_key FROM %s WHERE payload ->> 'int' = 'false'".formatted(tableName);
        List<String> erroneousEventKeys = jdbcTemplate.queryForList(findErroneousSql, String.class);
        output.append("Fant %s feilaktige records i %s\n\n".formatted(erroneousEventKeys.size(), tableName));

        if (erroneousEventKeys.isEmpty()) {
            output.append("Ingen feilaktige records å reparere.\n");
            return output.toString();
        }

        // Step 3: Match erroneous records with correct Kafka data and update
        int matchedCount = 0;
        int unmatchedCount = 0;
        int updatedCount = 0;

        for (String eventKey : erroneousEventKeys) {
            String correctPayload = kafkaMessages.get(eventKey);
            if (correctPayload == null) {
                unmatchedCount++;
                output.append("IKKE FUNNET i Kafka: event_key=%s\n".formatted(eventKey));
                continue;
            }

            matchedCount++;
            if (dryRun) {
                output.append("DRYRUN ville oppdatert: event_key=%s, payload_preview=%s\n".formatted(
                    eventKey,
                    correctPayload.length() > 200 ? correctPayload.substring(0, 200) + "..." : correctPayload
                ));
            } else {
                String updateSql = "UPDATE %s SET payload = to_jsonb(?::json) WHERE event_key = ?".formatted(tableName);
                int rowsAffected = jdbcTemplate.update(updateSql, correctPayload, eventKey);
                if (rowsAffected > 0) {
                    updatedCount++;
                }
                output.append("Oppdatert: event_key=%s (rows=%s)\n".formatted(eventKey, rowsAffected));
            }
        }

        output.append("\n=== Oppsummering ===\n");
        output.append("Totalt feilaktige records: %s\n".formatted(erroneousEventKeys.size()));
        output.append("Matchet med Kafka: %s\n".formatted(matchedCount));
        output.append("Ikke funnet i Kafka: %s\n".formatted(unmatchedCount));
        if (dryRun) {
            output.append("DRYRUN - ville oppdatert: %s records\n".formatted(matchedCount));
        } else {
            output.append("Oppdatert: %s records\n".formatted(updatedCount));
        }

        return output.toString();
    }

    // package-private for testability
    Map<String, String> readKafkaTopicFromOffset(String topic, long fromOffset) {
        Map<String, String> messages = new HashMap<>();
        try (var kafkaConsumer = new KafkaConsumer<String, String>(kafkaProperties.buildConsumerProperties())) {
            TopicPartition topicPartition = new TopicPartition(topic, 0);
            kafkaConsumer.assign(List.of(topicPartition));

            kafkaConsumer.seekToEnd(List.of(topicPartition));
            long endOffset = kafkaConsumer.position(topicPartition);
            kafkaConsumer.seek(topicPartition, fromOffset);

            log.info("Reading Kafka topic {} from offset {} to end offset {}", topic, fromOffset, endOffset);

            while (true) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(5));
                if (records.isEmpty()) {
                    break;
                }
                for (ConsumerRecord<String, String> record : records.records(topicPartition)) {
                    if (record.key() != null && record.value() != null) {
                        messages.put(record.key(), record.value());
                    }
                }
                if (kafkaConsumer.position(topicPartition) >= endOffset) {
                    break;
                }
            }
        }
        log.info("Read {} messages from Kafka topic {}", messages.size(), topic);
        return messages;
    }

    // package-private for testability
    String resolveTopicName(EventSourceTable table) {
        return switch (table) {
            case RESSURS -> ressursEventsourceTopic;
            case ORGENHET -> orgenhetEventsourceTopic;
            case ORGENHET_LEDER -> orgenhetLederEventsourceTopic;
            case ORGTILKNYTNING -> orgtilknytningEventsourceTopic;
            case ORGANISERING -> organiseringEventsourceTopic;
            case AUTHORIZE -> authorizeEventsourceTopic;
        };
    }

    // package-private for testability
    long resolveOffset(EventSourceTable table) {
        return switch (table) {
            case RESSURS -> ressursOffset;
            case ORGENHET -> orgenhetOffset;
            case ORGENHET_LEDER -> orgenhetLederOffset;
            case ORGTILKNYTNING -> orgtilknytningOffset;
            case ORGANISERING -> organiseringOffset;
            case AUTHORIZE -> authorizeOffset;
        };
    }
}

