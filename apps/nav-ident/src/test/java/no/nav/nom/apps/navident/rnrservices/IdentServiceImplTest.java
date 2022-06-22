package no.nav.nom.apps.navident.rnrservices;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.idgenerator.NavidGenerator;
import no.nav.nom.apps.navident.pdl.PdlService;
import no.nav.nom.apps.navident.rnrservices.util.DomainTestData;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import java.util.Map;

@ActiveProfiles({"test", "log-console"})
@SpringBootTest
@Slf4j
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IdentServiceImplTest {

    @Value("${test.rnr-topic}")  private String topic;

    @MockBean  private DataSource dataSource;
    @MockBean private PdlService pdlService;
    @MockBean private NavidGenerator navidGenerator;
    @Autowired private DomainTestData domainTestData;

    @Autowired private Map<String, Navident> navidenter;

    @BeforeEach
    public void resetNavidenterDomainData() { domainTestData.prepNavidenter(navidenter); }
}