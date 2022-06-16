package no.nav.nom.apps.navident.eventsource;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.domain.Periode;
import no.nav.nom.apps.navident.domain.PersonidType;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@ActiveProfiles({"test", "log-console"})
@SpringBootTest
@Slf4j
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
class EventSourceServiceTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public KafkaConsumer<String, String> stringValueConsumer(KafkaProperties properties) {
            properties.setClientId("plaintextConsumer");
            properties.getConsumer().setValueDeserializer(org.apache.kafka.common.serialization.StringDeserializer.class);
            return new KafkaConsumer<>(properties.buildConsumerProperties());
        }
    }

    @Autowired private KafkaConsumer<String, String> stringValueConsumer;
    @Autowired private EventSourceService eventSourceService;
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

        List<Periode> perioder = navidenter.get(navIdent).getAktivePerioder();
        assertEquals(1, perioder.size());
        assertEquals(LocalDate.now(), perioder.get(0).getFraOgMed());
        assertEquals(null, perioder.get(0).getTilOgMed());
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

        List<Periode> perioder = navidenter.get(navIdent).getAktivePerioder();
        assertEquals(1, perioder.size());
        assertEquals(LocalDate.now(), perioder.get(0).getFraOgMed());
        assertEquals(null, perioder.get(0).getTilOgMed());
    }

    @Test
    public void opprettNavIdentSomFinnesEvent() {
        String navIdent = "A000001";
        String personid = "01019900001";
        String personid2 = "01019900002";
        SourceEvent event = getOpprettEvent(navIdent, personid);
        SourceEvent event2 = getOpprettEvent(navIdent, personid2);

        kafkaTemplate.send(topic, event.payload.navident, event);
        kafkaTemplate.send(topic, event.payload.navident, event2); // Forventer at denne ikke behandles - med kun en error log

        await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> navidenter.containsKey(navIdent));

        assertEquals(navidenter.size(), 1);

        List<Periode> perioder = navidenter.get(navIdent).getAktivePerioder();
        assertEquals(1, perioder.size());
        assertEquals(LocalDate.now(), perioder.get(0).getFraOgMed());
        assertEquals(null, perioder.get(0).getTilOgMed());
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

        List<Periode> perioder = navidenter.get(navIdent).getAktivePerioder();
        assertEquals(1, perioder.size());
        assertEquals(LocalDate.now(), perioder.get(0).getFraOgMed());
        assertEquals(null, perioder.get(0).getTilOgMed());
    }

    @Test
    public void deaktiverNavIdentEvent() throws InterruptedException {
        String navIdent = "A000002";
        String personid = "01019900002";
        LocalDate fraDato = LocalDate.now().minusMonths(2);
        LocalDate tilDato = LocalDate.now();
        SourceEvent opprett = getOpprettEvent(navIdent, personid, fraDato );
        SourceEvent deaktiver = getDeaktiverEvent(navIdent, personid, tilDato);

        kafkaTemplate.send(topic, navIdent, opprett);
        kafkaTemplate.send(topic, navIdent, deaktiver);

        await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> navidenter.containsKey(navIdent));
        await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() ->!navidenter.get(navIdent).isActive(tilDato.plusDays(1)));

        List<Periode> perioder = navidenter.get(navIdent).getAktivePerioder();
        assertEquals(1, perioder.size());
        assertEquals(fraDato, perioder.get(0).getFraOgMed());
        assertEquals(tilDato, perioder.get(0).getTilOgMed());
    }

    @Test
    public void reaktiverNavIdentEvent() throws InterruptedException {
        String navIdent = "A000001";
        String personid = "01019900001";
        LocalDate fraDato = LocalDate.now().minusMonths(6);
        LocalDate tilDato = LocalDate.now().minusMonths(2);
        LocalDate reaktiverDato = LocalDate.now();
        SourceEvent opprett = getOpprettEvent(navIdent, personid, fraDato );
        SourceEvent deaktiver = getDeaktiverEvent(navIdent, personid, tilDato);
        SourceEvent reaktiver = getReaktiverEvent(navIdent, personid, reaktiverDato);

        kafkaTemplate.send(topic, navIdent, opprett);
        kafkaTemplate.send(topic, navIdent, deaktiver);
        kafkaTemplate.send(topic, navIdent, reaktiver);

        await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> navidenter.containsKey(navIdent));
        await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> navidenter.get(navIdent).getAktivePerioder().size() > 1);

        List<Periode> perioder = navidenter.get(navIdent).getAktivePerioder();
        assertEquals(2, perioder.size());
        assertEquals(fraDato, perioder.get(0).getFraOgMed());
        assertEquals(tilDato, perioder.get(0).getTilOgMed());
        assertEquals(reaktiverDato, perioder.get(1).getFraOgMed());
    }

    private SourceEvent getOpprettEvent(String navid, String personid) {
        return getOpprettEvent(navid, personid, LocalDate.now());
    }

    private SourceEvent getOpprettEvent(String navid, String personid, LocalDate fraDato) {
        return SourceEvent.builder()
                .bestiller("Test-bestiller")
                .event(EventType.OPPRETT)
                .eventTS(LocalDateTime.now())
                .dato(fraDato)
                .payload(
                        EventPayload.builder()
                                .identType(IdentType.STANDARD)
                                .navident(navid)
                                .personid(personid)
                                .personidtype(PersonidType.PDL)
                                .build()
                ).build();
    }

    private SourceEvent getDeaktiverEvent(String navid, String personid, LocalDate tilDato) {
        return SourceEvent.builder()
                .bestiller("Test-bestiller")
                .event(EventType.DEAKTIVER)
                .eventTS(LocalDateTime.now())
                .dato(tilDato)
                .payload(
                        EventPayload.builder()
                                .identType(IdentType.STANDARD)
                                .navident(navid)
                                .personid(personid)
                                .personidtype(PersonidType.PDL)
                                .build()
                ).build();
    }

    private SourceEvent getReaktiverEvent(String navid, String personid, LocalDate reaktiverDato) {
        return SourceEvent.builder()
                .bestiller("Test-bestiller")
                .event(EventType.REAKTIVER)
                .eventTS(LocalDateTime.now())
                .dato(reaktiverDato)
                .payload(
                        EventPayload.builder()
                                .identType(IdentType.STANDARD)
                                .navident(navid)
                                .personid(personid)
                                .personidtype(PersonidType.PDL)
                                .build()
                ).build();
    }

    private SourceEvent getReaktiverEvent(String navid, String personid) {
        return SourceEvent.builder()
                .bestiller("Test-bestiller")
                .event(EventType.SLETT)
                .eventTS(LocalDateTime.now())
                .payload(
                        EventPayload.builder()
                                .identType(IdentType.STANDARD)
                                .navident(navid)
                                .personid(personid)
                                .personidtype(PersonidType.PDL)
                                .build()
                ).build();
    }

}