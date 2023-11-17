package no.nav.nom.apps.nomremedyintegrasjon;

import io.getunleash.Unleash;
import no.nav.nom.apps.nomremedyintegrasjon.api.FysiskBrukerstedReadOnlyRepository;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.NomIntegrasjonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@ActiveProfiles("test")
@SpringBootTest(
    classes = NomRemedyIntegrasjonStarter.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = {MockOAuth2ServerInitializer.class})
@EmbeddedKafka(partitions = 1, topics = {"org.nom.ressurs-eventsource"}, controlledShutdown = true)
public abstract class TestBase {

    @MockBean
    protected Unleash unleash;

    @Autowired
    protected NomIntegrasjonRepository nomIntegrasjonRepository;

    @MockBean
    protected FysiskBrukerstedReadOnlyRepository fysiskBrukerstedReadOnlyRepository;

    protected final String ressursEventSourceTopic = "org.nom.ressurs-eventsource";

}
