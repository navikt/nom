package no.nav.nom.apps.api;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.organisering.DvhOrganiseringMockUtils;
import no.nav.nom.apps.api.pdlutils.PdlUtils;
import no.nav.nom.apps.api.postgresql.GenericStorage;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.gql.NomGraphQlAutoConfiguration;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateAutoconfiguration;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.StreamUtils;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(

    classes = { TestConfig.class, MasteredRessursStateAutoconfiguration.class, NomGraphQlAutoConfiguration.class, TestBase.ConfigureRemedy.class },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "nom.libs.state.idle-event-interval-seconds: 1",
        "nom.libs.state.mastered-ressurs.enabled: true",
        "nom.libs.state.mastered-ressurs.state-topic: org.nom.nom-mastered-ressurs",
        "nom.libs.state.mastered-ressurs.listener-id: nomStateMasteredRessursListener__test"
    }
)
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"nom-state-v1", "org.nom.ressurs-state-v1", "org.nom.orgenhet-state-v1", 
        "org.nom.nom-mastered-ressurs", "org.nom.organisering-eventsource", "org.nom.orgtilknytning-eventsource"}, 
        controlledShutdown = true)
@ContextConfiguration(initializers = {MockOAuth2ServerInitializer.class})
@EnableWireMock({
    @ConfigureWireMock(
        filesUnderDirectory = "src/test/resources")
})
public abstract class TestBase {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected MockOAuth2Server mockOAuth2Server;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Autowired
    protected DvhRepository dvhRepository;

    @Autowired
    private GenericStorage genericStorage;

    @Autowired
    public DvhOrgEnhetMockUtils dvhOrgenhetMockUtils;

    @Autowired
    public DvhOrganiseringMockUtils dvhOrganiseringMockUtils;

    @Autowired
    public DvhRessursUtils dvhRessursUtils;

    @Autowired
    protected PdlUtils pdlUtils;

    @Autowired
    protected KafkaUtils kafkaUtils;

    @Autowired
    protected RessursStateRepository ressursStateRepository;

    @MockitoBean
    protected Unleash unleash;

    @Autowired
    protected OrgEnhetStateRepository orgEnhetStateRepository;

    @Autowired
    protected OrganiseringStateRepository organiseringStateRepository;

    @Autowired
    protected OrgtilknytningStateRepository orgtilknytningStateRepository;

    @Autowired
    protected OrgEnhetLederStateRepository orgEnhetLederStateRepository;

    @Autowired
    protected MasteredRessursStateRepository masteredRessursStateRepository;

    @MockitoBean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    @BeforeEach
    void reset() {
        dvhRepository.clearRepository();
        orgEnhetStateRepository.clear();
        organiseringStateRepository.clear();
        genericStorage.deleteAll();
        dvhOrgenhetMockUtils.createEmptyResponse();
        kafkaUtils.clearKafka();
        ressursStateRepository.clear();
        dvhRessursUtils.createEmptyResponse();
        orgEnhetLederStateRepository.clear();
    }

    @BeforeEach
    void mockOAuth2AuthorizedClientManager() {
        OAuth2AuthorizedClient oAuth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);
        when(oAuth2AuthorizedClientManager.authorize(any())).thenReturn(oAuth2AuthorizedClient);
        lenient().when(oAuth2AuthorizedClient.getAccessToken()).thenReturn(
            new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "tokenValue",
                Instant.now(),
                Instant.now().plusSeconds(1000L)
            )
        );
    }

    /**
     * Loads a given resource from the test classpath using ResourceLoader
     *
     * @param location path to the resource
     * @return The string content of the resource
     * @throws IOException
     */
    protected String loadResourceFromClasspath(String location) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + location);
        return loadResource(resource);
    }

    private String loadResource(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }



    protected void saveOrgtilknytninger(List<OrgtilknytningState> orgtilknytningStateList) {
        orgtilknytningStateList.forEach(o -> orgtilknytningStateRepository.update(o.fid(), o));
    }

    protected void saveOrgenheter(List<OrgEnhetState> orgEnhetStateList) {
        orgEnhetStateList.forEach(o -> orgEnhetStateRepository.update(o));
    }

    protected void clearOrgtilknytninger() {
        orgtilknytningStateRepository.clear();
    }

    protected void saveRessurser(List<RessursState> ressursStateList) {
        ressursStateList.forEach(r -> ressursStateRepository.update(r));
    }

    static class ConfigureRemedy{
//        @Bean
//        RemedyIdResolverInterface remedyIdResolver() {
//            return (x) -> "remedy-"+x;
//        }
    }

}
