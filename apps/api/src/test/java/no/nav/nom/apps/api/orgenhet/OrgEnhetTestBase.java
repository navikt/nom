package no.nav.nom.apps.api.organisering;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.DvhOrgEnhetMockUtils;
import no.nav.nom.apps.api.KafkaUtils;
import no.nav.nom.apps.api.MockOAuth2ServerInitializer;
import no.nav.nom.apps.api.orgenhet.OrgEnhetTestConfig;
import no.nav.nom.libs.state.organisering.OrganiseringStateAutoconfiguration;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateAutoconfiguration;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@ActiveProfiles("test")
@SpringBootTest(
    properties = {
        "nom.libs.state.idle-event-interval-seconds: 1",
        "nom.libs.state.orgenhet.enabled: true",
        "nom.libs.state.orgenhet.state-topic: org.nom.intern-orgenhet-state",
        "nom.libs.state.orgenhet.listener-id: nomStateOrgenhetListener_test",
        "nom.libs.state.organisering.enabled: true",
        "nom.libs.state.organisering.state-topic: org.nom.intern-organisering-state",
        "nom.libs.state.organisering.listener-id: nomStateOrganiseringListener_test"
    }
)
@ContextConfiguration(classes= {OrgEnhetTestConfig.class, OrgEnhetStateAutoconfiguration.class, OrganiseringStateAutoconfiguration.class}, initializers = MockOAuth2ServerInitializer.class)
@EmbeddedKafka(partitions = 1,  topics = {"org.nom.intern-orgenhet-state", "org.nom.intern-organisering-state"},  controlledShutdown = true)
@AutoConfigureWireMock(port = 0)
public class OrgEnhetTestBase {
    @Autowired
    public DvhOrgEnhetMockUtils dvhOrgenhetMockUtils;

    @MockBean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    @Autowired
    public Unleash unleash;

    @Autowired
    public OrganiseringStateRepository organiseringStateRepository;

    @Autowired
    public OrgEnhetStateRepository orgEnhetStateRepository;

    @Autowired
    KafkaUtils kafkaUtils;

    @BeforeEach
    void setup() {
        kafkaUtils.clearKafka();
        dvhOrgenhetMockUtils.createEmptyResponse();
        orgEnhetStateRepository.clear();
        organiseringStateRepository.clear();
    }
}

