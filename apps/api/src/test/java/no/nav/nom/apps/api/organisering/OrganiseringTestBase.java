package no.nav.nom.apps.api.organisering;

import no.nav.nom.apps.api.MockOAuth2ServerInitializer;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventKafkaConfig;
import no.nav.nom.libs.state.ressurs.RessursStateAutoconfiguration;
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
        "nom.libs.state.ressurs.enabled: true",
        "nom.libs.state.ressurs.state-topic: org.nom.intern-ressurs-state",
        "nom.libs.state.ressurs.listener-id: nomStateRessursListener_test"
    }
)
@ContextConfiguration(
    classes= {
        OrganiseringTestConfig.class,
        OrganiseringEventKafkaConfig.class,
        RessursStateAutoconfiguration.class
    },
    initializers = MockOAuth2ServerInitializer.class)
@EmbeddedKafka(partitions = 1,  topics = {"org.nom.organisering-eventsource"},  controlledShutdown = true)
@AutoConfigureWireMock(port = 0)
public class OrganiseringTestBase {
    @Autowired
    public DvhOrganiseringMockUtils dvhOrganiseringMockUtils;

    @MockBean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;
}

