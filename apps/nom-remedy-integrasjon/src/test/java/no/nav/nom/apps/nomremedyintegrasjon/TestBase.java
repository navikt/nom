package no.nav.nom.apps.nomremedyintegrasjon;

import io.getunleash.Unleash;
import no.nav.nom.apps.nomremedyintegrasjon.api.FysiskBrukerstedReadOnlyRepository;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.NomIntegrasjonRepository;
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
    classes = NomRemedyIntegrasjonStarter.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)

@AutoConfigureWireMock(port = 0)
@ContextConfiguration(initializers = {MockOAuth2ServerInitializer.class})
@EmbeddedKafka(partitions = 1, topics = {"org.nom.ressurs-eventsource", "org.nom.orgtilknytning-eventsource"}, controlledShutdown = true)
public abstract class TestBase {

    @MockBean
    protected OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    @MockBean
    protected Unleash unleash;

    @Autowired
    protected NomIntegrasjonRepository nomIntegrasjonRepository;

    @MockBean
    protected FysiskBrukerstedReadOnlyRepository fysiskBrukerstedReadOnlyRepository;

    protected final String ressursEventSourceTopic = "org.nom.ressurs-eventsource";
    protected final String orgtilknytningEventSourceTopic = "org.nom.orgtilknytning-eventsource";

}
