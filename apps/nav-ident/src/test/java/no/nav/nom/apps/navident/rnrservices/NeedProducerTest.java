package no.nav.nom.apps.navident.rnrservices;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.domain.Personid;
import no.nav.nom.apps.navident.domain.PersonidType;
import no.nav.nom.apps.navident.eventsource.SourceEvent;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles({"test", "log-console"})
@SpringBootTest
@Slf4j
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9093", "port=9093" })
class NeedProducerTest {

//    @Autowired private KafkaProperties properties;
    @Autowired private NeedProducer needProducer;
    @MockBean private DataSource dataSource;

    List<Need> needsRead = new ArrayList<>();

    @BeforeEach
    private void emptyNeedsRead() { needsRead.clear(); }

    @Test
    public void createNeed() {
        String id = "A000001";
        needProducer.produceNeed(id, IdentType.STANDARD);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> needsRead.size() >= 1);

        assertEquals(1, needsRead.size());
        assertEquals(id, needsRead.get(0).personid);
        assertEquals(IdentType.STANDARD, needsRead.get(0).identType);
    }

    @KafkaListener(
            id = "rnrTestListener",
            topics = "${app.kafka.topic.rnr.name}")
    public void listen(Need need) {
        needsRead.add(need);
        log.debug("Need consumed from rnr topic: %s".formatted(need));
    }
}