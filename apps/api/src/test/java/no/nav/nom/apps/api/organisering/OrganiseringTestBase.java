package no.nav.nom.apps.api.organisering;

import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventKafkaConfig;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.orgtilknytning.MasseflyttOrgtilknytningerService;
import no.nav.nom.apps.api.telefon.TelefonService;
import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.organisering.OrganiseringStateAutoconfiguration;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhetleder.NomStateOrgEnhetLederListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateAutoconfiguration;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles("test")
@SpringBootTest(
    properties = {
        "nom.libs.state.idle-event-interval-seconds: 1",
        "nom.libs.state.ressurs.enabled: true",
        "nom.libs.state.ressurs.state-topic: org.nom.intern-ressurs-state",
        "nom.libs.state.ressurs.listener-id: nomStateRessursListener_test",
        "nom.libs.state.organisering.enabled: true",
        "nom.libs.state.organisering.state-topic: org.nom.intern-organisering-state",
        "nom.libs.state.organisering.listener-id: nomStateOrganiseringListener_test"
    }
)
@ContextConfiguration(
    classes= {
        OrganiseringTestConfig.class,
        OrganiseringEventKafkaConfig.class,
        RessursStateAutoconfiguration.class,
        OrganiseringStateAutoconfiguration.class,
    },
    initializers = MockOAuth2ServerInitializer.class)
@EmbeddedKafka(partitions = 1,  topics = {"org.nom.organisering-eventsource", "org.nom.intern-ressurs-state"},  controlledShutdown = true)
@EnableWireMock({
    @ConfigureWireMock(
        filesUnderDirectory = "src/test/resources")
})
public class OrganiseringTestBase {
    @Autowired
    public DvhOrganiseringMockUtils dvhOrganiseringMockUtils;

    @MockitoBean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    @MockitoBean
    MasseflyttOrgtilknytningerService masseflyttOrgtilknytningerService;

    @MockitoBean
    TelefonService telefonService;

    @MockitoBean
    OrgEnhetLederService orgEnhetLederService;

    @MockitoBean
    NomStateRessursListener nomStateRessursListener;

    @MockitoBean
    NomStateOrgEnhetListener nomStateOrgEnhetListener;

    @MockitoBean
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener;

    @MockitoBean
    NomStateOrganiseringListener nomStateOrganiseringListener;

    @MockitoBean
    NomStateMasteredRessursListener nomStateMasteredRessursListener;

    @MockitoBean
    NomStateDelegertLederListener nomStateDelegertLederListener;

    @MockitoBean
    NomStateOrgEnhetLederListener nomStateOrgEnhetLederListener;
}

