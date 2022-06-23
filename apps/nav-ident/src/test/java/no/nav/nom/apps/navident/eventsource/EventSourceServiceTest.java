package no.nav.nom.apps.navident.eventsource;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.domain.PersonidType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@ActiveProfiles({"test", "log-console"})
@SpringBootTest
@Slf4j
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EventSourceServiceTest {
    @Autowired private KafkaTemplate<String, SourceEvent> kafkaTemplate;
    @Autowired private Map<String, Navident> navidenter;
    @MockBean  private DataSource dataSource;
    @Value("${test.topic}")  private String topic;

    @BeforeEach
    public void resetDomain() {
        navidenter.clear();
    }

    @Test
    public void opprettNavIdentEvent() {
        String navIdent = "A000001";
        String personid = "01019900001";
        SourceEvent event = getOpprettEvent(navIdent, personid);

        kafkaTemplate.send(topic, event.payload.navident, event);

        await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> navidenter.containsKey(navIdent));
    }

    @Test
    public void opprett3NavIdenterEvent() {
        String navIdent = "A000001";
        String personid = "01019900001";
        String navIdent2 = "A000002";
        String personid2 = "01019900002";
        String navIdent3 = "A000003";
        String personid3 = "01019900003";

        SourceEvent event = getOpprettEvent(navIdent, personid);
        SourceEvent event2 = getOpprettEvent(navIdent2, personid2);
        SourceEvent event3 = getOpprettEvent(navIdent3, personid3);

        kafkaTemplate.send(topic, event.payload.navident, event);
        kafkaTemplate.send(topic, event2.payload.navident, event2);
        kafkaTemplate.send(topic, event3.payload.navident, event3);

        await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> navidenter.containsKey(navIdent));
        await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> navidenter.containsKey(navIdent2));
        await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> navidenter.containsKey(navIdent3));

        assertEquals(3, navidenter.size());
        assertEquals(personid3, navidenter.get(navIdent3).getGjeldendePersonid());
    }

    @Test
    public void opprettNavIdentSomFinnesEvent() {
        String navIdent = "A000001";
        String personid = "01019900001";
        String personid2 = "01019900002";
        SourceEvent event = getOpprettEvent(navIdent, personid);
        SourceEvent event2 = getOpprettEvent(navIdent, personid2);

        kafkaTemplate.send(topic, event.payload.navident, event);
        kafkaTemplate.send(topic, event.payload.navident, event2); // Forventer at denne erstatter forrige info

        await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> navidenter.containsKey(navIdent));

        assertEquals(navidenter.size(), 1);
        assertEquals(personid2, navidenter.get(navIdent).getGjeldendePersonid());
    }

    @Test
    public void opprettNavIdentHvorPersonidFinnesEvent() {
        String navIdent = "A000001";
        String navIdent2 = "A000002";
        String personid = "01019900001";
        SourceEvent event = getOpprettEvent(navIdent, personid);
        SourceEvent event2 = getOpprettEvent(navIdent2, personid);

        kafkaTemplate.send(topic, event.payload.navident, event);
        kafkaTemplate.send(topic, event2.payload.navident, event2); // Forventer at denne ikke behandles - med kun en error log

        await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> navidenter.containsKey(navIdent));

        assertEquals(1, navidenter.size());

    }

    private SourceEvent getOpprettEvent(String navid, String personid) {
        return getOpprettEvent(navid, personid, LocalDate.now());
    }

    private SourceEvent getOpprettEvent(String navid, String personid, LocalDate fraDato) {
        return SourceEvent.builder()
                .bestiller("Test-bestiller")
                .event(EventType.OPPRETT)
                .eventTS(LocalDateTime.now())
                .payload(
                        EventPayload.builder()
                                .identType(IdentType.STANDARD)
                                .navident(navid)
                                .personid(personid)
                                .personidtype(PersonidType.PDL)
                                .build()
                ).build();}

}