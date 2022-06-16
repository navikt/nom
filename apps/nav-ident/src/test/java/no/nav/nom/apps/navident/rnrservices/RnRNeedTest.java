package no.nav.nom.apps.navident.rnrservices;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.IdentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles({"test", "log-console"})
@SpringBootTest
@Slf4j
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9093", "port=9093" })
class RnRNeedTest {

    @Autowired private NeedProducer needProducer;
    @MockBean private DataSource dataSource;
    @Value("${test.rnr-topic}")  private String topic;
    @Autowired private RnRTestListener rnrTestListener;
    @Autowired private RnRTestNeedProducer rnrTestNeedProducer;

    @BeforeEach
    public void emptyNeedsRead() { rnrTestListener.needsRead.clear(); }

    @Test
    public void createNeed() {
        String id = "01019912345";
        needProducer.produceNeed(id, IdentType.STANDARD);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.needsRead.size() >= 1);

        List<Need> needs = rnrTestListener.needsRead.stream().filter(n -> n.visited == 0).collect(Collectors.toList());
        assertEquals(1, needs.size());
        assertEquals(id,needs.get(0).personid);
        assertEquals(IdentType.STANDARD, needs.get(0).identType);
    }


    @Test
    public void personOppslagId_NeedStandardUtenPDL() {
        String id = "01019912345";
        Need need = Need.builder()
                .personid(id)
                .identType(IdentType.STANDARD)
                .build();

        rnrTestNeedProducer.publish(need);


        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.needsRead.size() >= 1);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).pollDelay(200, MILLISECONDS).until(
                () -> !rnrTestListener.needsRead.stream().anyMatch(n -> n.visited > 0 && n.eksisterendeId != null )
        );

        // Ingen behandling, men forvent også logg fra PersonidOppslagNeedListener
    }

    @Test
    public void personOppslagId_NeedRobot_FinnesIkke() {
        String id = "X12345";
        Need need = Need.builder()
                .personid(id)
                .identType(IdentType.ROBOT)
                .build();

        rnrTestNeedProducer.publish(need);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(() -> rnrTestListener.needsRead.size() >= 1);

        Awaitility.await().atMost(1, SECONDS).pollInterval(10, MILLISECONDS).until(
                () -> rnrTestListener.needsRead.stream().anyMatch(n -> n.visited > 0 &&  n.eksisterendeId != null && n.eksisterendeId.length() == 0)
        );

        Need populatedNeed = rnrTestListener.needsRead.stream().filter(n -> n.visited > 0 &&  n.eksisterendeId != null && n.eksisterendeId.length() == 0).findFirst().get();

        assertEquals(id, populatedNeed.personid);
        assertEquals("", populatedNeed.eksisterendeId);
    }
}