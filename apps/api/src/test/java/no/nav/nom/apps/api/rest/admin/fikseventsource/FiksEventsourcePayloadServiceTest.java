package no.nav.nom.apps.api.rest.admin.fikseventsource;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@EmbeddedKafka(partitions = 1, topics = {FiksEventsourcePayloadServiceTest.TEST_TOPIC})
class FiksEventsourcePayloadServiceTest {

    static final String TEST_TOPIC = "org.nom.test-eventsource";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private FiksEventsourcePayloadService service;
    private KafkaProducer<String, String> kafkaProducer;

    @BeforeEach
    void setUp() {
        // Build KafkaProperties pointing to the embedded broker
        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.setBootstrapServers(embeddedKafkaBroker.getBrokersAsString().lines().toList());
        kafkaProperties.getSecurity().setProtocol("PLAINTEXT");

        service = spy(new FiksEventsourcePayloadService(kafkaProperties, jdbcTemplate));

        // Stub topic and offset resolution — all tables resolve to our test topic at offset 0
        doReturn(TEST_TOPIC).when(service).resolveTopicName(any());
        lenient().doReturn(0L).when(service).resolveOffset(any());

        // Set up a Kafka producer for writing test messages
        Map<String, Object> producerProps = Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        );
        kafkaProducer = new KafkaProducer<>(producerProps);

        // Clean up all eventsource tables before each test
        jdbcTemplate.execute("DELETE FROM T_RESSURS_EVENTSOURCE");
        jdbcTemplate.execute("DELETE FROM T_ORGENHET_EVENTSOURCE");
        jdbcTemplate.execute("DELETE FROM T_ORGENHET_LEDER_EVENTSOURCE");
        jdbcTemplate.execute("DELETE FROM T_ORGTILKNYTNING_EVENTSOURCE");
        jdbcTemplate.execute("DELETE FROM T_ORGANISERING_EVENTSOURCE");
        jdbcTemplate.execute("DELETE FROM T_AUTHORIZE_EVENTSOURCE");
    }

    private void sendToKafka(String key, String value) {
        try {
            kafkaProducer.send(new ProducerRecord<>(TEST_TOPIC, key, value)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to send message to Kafka", e);
        }
    }

    private void insertErroneousRecord(String tableName, String eventKey) {
        String erroneousPayload = """
            {"int": false, "long": false, "null": false, "pojo": false, "array": false, "empty": false,
             "float": false, "short": false, "binary": false, "double": false, "number": false,
             "object": true, "string": false, "boolean": false, "textual": false, "nodeType": "OBJECT",
             "container": true, "valueNode": false, "bigDecimal": false, "bigInteger": false,
             "missingNode": false, "embeddedValue": false, "integralNumber": false, "floatingPointNumber": false}
            """;
        jdbcTemplate.update(
            "INSERT INTO %s (event_key, payload) VALUES (?, to_jsonb(?::json))".formatted(tableName),
            eventKey, erroneousPayload
        );
    }

    private void insertCorrectRecord(String tableName, String eventKey, String payload) {
        jdbcTemplate.update(
            "INSERT INTO %s (event_key, payload) VALUES (?, to_jsonb(?::json))".formatted(tableName),
            eventKey, payload
        );
    }

    /**
     * Returns the current end offset for the shared test topic (partition 0).
     * Useful for offset-based tests so they can compute relative offsets
     * regardless of how many messages were sent by previous tests.
     */
    private long getCurrentTopicEndOffset() {
        try (var consumer = new KafkaConsumer<String, String>(Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString(),
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        ))) {
            TopicPartition tp = new TopicPartition(TEST_TOPIC, 0);
            consumer.assign(List.of(tp));
            consumer.seekToEnd(List.of(tp));
            return consumer.position(tp);
        }
    }

    @Test
    @DisplayName("Dry run med feilaktige records som finnes i Kafka - skal rapportere hva som ville blitt oppdatert")
    void dryRunReportsMatchedRecords() {
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "event-key-1");
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "event-key-2");

        sendToKafka("event-key-1",
            """
            {"fid": "ressurs-01ABC", "type": "RessursOpprettetEventV1", "data": {"navident": "A990001"}}""");
        sendToKafka("event-key-2",
            """
            {"fid": "ressurs-02DEF", "type": "RessursOpprettetEventV1", "data": {"navident": "A990002"}}""");

        String result = service.repair(EventSourceTable.RESSURS, true);

        assertThat(result).contains("Dry run: true");
        assertThat(result).contains("Fant 2 feilaktige records");
        assertThat(result).contains("DRYRUN ville oppdatert: event_key=event-key-1");
        assertThat(result).contains("DRYRUN ville oppdatert: event_key=event-key-2");
        assertThat(result).contains("DRYRUN - ville oppdatert: 2 records");

        // Verify DB was NOT changed
        String payload = jdbcTemplate.queryForObject(
            "SELECT payload::text FROM T_RESSURS_EVENTSOURCE WHERE event_key = ?", String.class, "event-key-1"
        );
        assertThat(payload).contains("\"int\"");
    }

    @Test
    @DisplayName("Ekte kjøring oppdaterer feilaktige records med korrekt payload fra Kafka")
    void realRunUpdatesRecords() {
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "event-key-1");

        sendToKafka("event-key-1",
            """
            {"fid": "ressurs-01ABC", "type": "RessursOpprettetEventV1", "data": {"navident": "A990001"}}""");

        String result = service.repair(EventSourceTable.RESSURS, false);

        assertThat(result).contains("Dry run: false");
        assertThat(result).contains("Oppdatert: event_key=event-key-1 (rows=1)");
        assertThat(result).contains("Oppdatert: 1 records");

        // Verify DB was actually updated with correct payload
        String updatedPayload = jdbcTemplate.queryForObject(
            "SELECT payload::text FROM T_RESSURS_EVENTSOURCE WHERE event_key = ?", String.class, "event-key-1"
        );
        assertThat(updatedPayload).contains("ressurs-01ABC");
        assertThat(updatedPayload).contains("RessursOpprettetEventV1");
        assertThat(updatedPayload).doesNotContain("\"int\"");
    }

    @Test
    @DisplayName("Feilaktige records som ikke finnes i Kafka rapporteres som IKKE FUNNET")
    void unmatchedRecordsAreReported() {
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "event-key-1");
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "event-key-missing");

        sendToKafka("event-key-1",
            """
            {"fid": "ressurs-01ABC", "type": "RessursOpprettetEventV1"}""");

        String result = service.repair(EventSourceTable.RESSURS, true);

        assertThat(result).contains("IKKE FUNNET i Kafka: event_key=event-key-missing");
        assertThat(result).contains("Matchet med Kafka: 1");
        assertThat(result).contains("Ikke funnet i Kafka: 1");
    }

    @Test
    @DisplayName("Ingen feilaktige records gir tidlig retur med beskjed")
    void noErroneousRecordsReturnsEarly() {
        // Insert a correct record (not matching erroneous pattern)
        insertCorrectRecord("T_RESSURS_EVENTSOURCE", "event-key-ok",
            """
            {"fid": "ressurs-01", "type": "RessursOpprettetEventV1"}"""
        );

        String result = service.repair(EventSourceTable.RESSURS, true);

        assertThat(result).contains("Fant 0 feilaktige records");
        assertThat(result).contains("Ingen feilaktige records");
    }

    @Test
    @DisplayName("Lang payload i dry run blir trunkert i preview")
    void longPayloadIsTruncatedInDryRun() {
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "event-key-1");

        String longPayload = "{\"data\":\"" + "x".repeat(300) + "\"}";
        sendToKafka("event-key-1", longPayload);

        String result = service.repair(EventSourceTable.RESSURS, true);

        assertThat(result).contains("DRYRUN ville oppdatert: event_key=event-key-1");
        assertThat(result).contains("...");
        // Should not contain the full long payload
        assertThat(result).doesNotContain("x".repeat(300));
    }

    @Test
    @DisplayName("Blanding av matchede og umatchede records i ekte kjøring")
    void mixedMatchedAndUnmatchedInRealRun() {
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "mix-key-1");
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "mix-key-2");
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "mix-key-3");

        sendToKafka("mix-key-1",
            """
            {"fid": "ressurs-01", "type": "RessursOpprettetEventV1"}""");
        sendToKafka("mix-key-3",
            """
            {"fid": "ressurs-03", "type": "RessursOpprettetEventV1"}""");

        String result = service.repair(EventSourceTable.RESSURS, false);

        assertThat(result).contains("Totalt feilaktige records: 3");
        assertThat(result).contains("Matchet med Kafka: 2");
        assertThat(result).contains("Ikke funnet i Kafka: 1");
        assertThat(result).contains("Oppdatert: 2 records");
        assertThat(result).contains("IKKE FUNNET i Kafka: event_key=mix-key-2");

        // Verify updated records have correct payload
        String payload1 = jdbcTemplate.queryForObject(
            "SELECT payload::text FROM T_RESSURS_EVENTSOURCE WHERE event_key = ?", String.class, "mix-key-1"
        );
        assertThat(payload1).contains("ressurs-01");

        // Verify unmatched record still has erroneous payload
        String payload2 = jdbcTemplate.queryForObject(
            "SELECT payload::text FROM T_RESSURS_EVENTSOURCE WHERE event_key = ?", String.class, "mix-key-2"
        );
        assertThat(payload2).contains("\"int\"");
    }

    @Test
    @DisplayName("Reparering bruker riktig tabellnavn for hver EventSourceTable")
    void usesCorrectTableNamePerEnum() {
        for (EventSourceTable table : EventSourceTable.values()) {
            String result = service.repair(table, true);
            assertThat(result).contains("=== Reparering av %s ===".formatted(table.getTableName()));
            assertThat(result).contains("Fant 0 feilaktige records");
        }
    }

    @Test
    @DisplayName("Ekte kjøring oppdaterer korrekt i ulike eventsource-tabeller")
    void realRunWorksForDifferentTables() {
        insertErroneousRecord("T_ORGENHET_EVENTSOURCE", "orgenhet-event-1");

        sendToKafka("orgenhet-event-1",
            """
            {"fid": "orgenhet-01", "type": "OrgEnhetOpprettetEventV1", "data": {"navn": "TestEnhet"}}""");

        String result = service.repair(EventSourceTable.ORGENHET, false);

        assertThat(result).contains("=== Reparering av T_ORGENHET_EVENTSOURCE ===");
        assertThat(result).contains("Oppdatert: 1 records");

        String updatedPayload = jdbcTemplate.queryForObject(
            "SELECT payload::text FROM T_ORGENHET_EVENTSOURCE WHERE event_key = ?", String.class, "orgenhet-event-1"
        );
        assertThat(updatedPayload).contains("orgenhet-01");
        assertThat(updatedPayload).doesNotContain("\"int\"");
    }

    @Test
    @DisplayName("Kun meldinger fra angitt offset leses - meldinger før offset ignoreres")
    void onlyMessagesFromOffsetAreRead() {
        // Determine the current end offset before sending any messages for this test
        long baseOffset = getCurrentTopicEndOffset();

        // Send 5 messages to Kafka — at baseOffset+0, +1, +2, +3, +4
        sendToKafka("before-offset-key-1",
            """
            {"fid": "old-1", "type": "ShouldBeIgnored"}""");
        sendToKafka("before-offset-key-2",
            """
            {"fid": "old-2", "type": "ShouldBeIgnored"}""");
        sendToKafka("before-offset-key-3",
            """
            {"fid": "old-3", "type": "ShouldBeIgnored"}""");
        // Messages at baseOffset+3 and baseOffset+4 should be included
        sendToKafka("after-offset-key-1",
            """
            {"fid": "new-1", "type": "RessursOpprettetEventV1"}""");
        sendToKafka("after-offset-key-2",
            """
            {"fid": "new-2", "type": "RessursOpprettetEventV1"}""");

        // Insert erroneous records for ALL keys
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "before-offset-key-1");
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "before-offset-key-2");
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "before-offset-key-3");
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "after-offset-key-1");
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "after-offset-key-2");

        // Override the offset to baseOffset+3 — only messages at +3 and +4 should be read
        doReturn(baseOffset + 3).when(service).resolveOffset(any());

        String result = service.repair(EventSourceTable.RESSURS, true);

        // All 5 erroneous records should be found in the DB
        assertThat(result).contains("Fant 5 feilaktige records");

        // Only 2 messages should have been read from Kafka (from baseOffset+3)
        assertThat(result).contains("Lest 2 meldinger fra Kafka topic");

        // The 2 records matching Kafka messages from offset should be matched
        assertThat(result).contains("Matchet med Kafka: 2");
        assertThat(result).contains("Ikke funnet i Kafka: 3");

        // Messages before offset should NOT be found
        assertThat(result).contains("IKKE FUNNET i Kafka: event_key=before-offset-key-1");
        assertThat(result).contains("IKKE FUNNET i Kafka: event_key=before-offset-key-2");
        assertThat(result).contains("IKKE FUNNET i Kafka: event_key=before-offset-key-3");

        // Messages from offset should be matched
        assertThat(result).contains("DRYRUN ville oppdatert: event_key=after-offset-key-1");
        assertThat(result).contains("DRYRUN ville oppdatert: event_key=after-offset-key-2");
    }

    @Test
    @DisplayName("Offset parameter respekteres ved ekte kjøring - kun meldinger fra offset brukes til oppdatering")
    void offsetParameterRespectedInRealRun() {
        // Determine the current end offset before sending any messages for this test
        long baseOffset = getCurrentTopicEndOffset();

        // Send 3 messages: at baseOffset+0, +1, +2
        sendToKafka("skip-key",
            """
            {"fid": "skip", "type": "ShouldBeSkipped"}""");
        sendToKafka("skip-key-2",
            """
            {"fid": "skip-2", "type": "ShouldBeSkipped"}""");
        sendToKafka("include-key",
            """
            {"fid": "included", "type": "RessursOpprettetEventV1"}""");

        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "skip-key");
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "include-key");

        // Override offset to baseOffset+2 — only last message should be read
        doReturn(baseOffset + 2).when(service).resolveOffset(any());

        String result = service.repair(EventSourceTable.RESSURS, false);

        assertThat(result).contains("Lest 1 meldinger fra Kafka topic");
        assertThat(result).contains("Matchet med Kafka: 1");
        assertThat(result).contains("Ikke funnet i Kafka: 1");
        assertThat(result).contains("Oppdatert: 1 records");
        assertThat(result).contains("IKKE FUNNET i Kafka: event_key=skip-key");

        // Verify the included record was actually updated
        String updatedPayload = jdbcTemplate.queryForObject(
            "SELECT payload::text FROM T_RESSURS_EVENTSOURCE WHERE event_key = ?", String.class, "include-key"
        );
        assertThat(updatedPayload).contains("included");
        assertThat(updatedPayload).doesNotContain("\"int\"");

        // Verify the skipped record still has erroneous payload
        String skippedPayload = jdbcTemplate.queryForObject(
            "SELECT payload::text FROM T_RESSURS_EVENTSOURCE WHERE event_key = ?", String.class, "skip-key"
        );
        assertThat(skippedPayload).contains("\"int\"");
    }

    @Test
    @DisplayName("Korrekte records i tabellen påvirkes ikke av reparering")
    void correctRecordsAreNotAffected() {
        insertCorrectRecord("T_RESSURS_EVENTSOURCE", "correct-key",
            """
            {"fid": "ressurs-ok", "type": "RessursOpprettetEventV1"}"""
        );
        insertErroneousRecord("T_RESSURS_EVENTSOURCE", "erroneous-key");

        sendToKafka("erroneous-key",
            """
            {"fid": "ressurs-fixed", "type": "RessursOpprettetEventV1"}""");

        String result = service.repair(EventSourceTable.RESSURS, false);

        assertThat(result).contains("Fant 1 feilaktige records");
        assertThat(result).contains("Oppdatert: 1 records");

        // Verify the correct record was not touched
        String correctPayload = jdbcTemplate.queryForObject(
            "SELECT payload::text FROM T_RESSURS_EVENTSOURCE WHERE event_key = ?", String.class, "correct-key"
        );
        assertThat(correctPayload).contains("ressurs-ok");

        // Verify the erroneous record was fixed
        String fixedPayload = jdbcTemplate.queryForObject(
            "SELECT payload::text FROM T_RESSURS_EVENTSOURCE WHERE event_key = ?", String.class, "erroneous-key"
        );
        assertThat(fixedPayload).contains("ressurs-fixed");
    }
}
