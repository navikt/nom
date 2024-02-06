package no.nav.nom.apps.nomremedyintegrasjon;

import com.nimbusds.jose.JOSEObjectType;
import io.getunleash.Unleash;
import no.nav.nom.apps.nomremedyintegrasjon.api.FysiskBrukerstedReadOnlyRepository;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.NomIntegrasjonRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(
    classes = NomRemedyIntegrasjonStarter.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)

@AutoConfigureWireMock(port = 0)
@ContextConfiguration(initializers = {MockOAuth2ServerInitializer.class})
@EmbeddedKafka(partitions = 1, topics = {"org.nom.ressurs-eventsource", "org.nom.orgtilknytning-eventsource"}, controlledShutdown = true)
public abstract class TestBase {

    public static final String LOGGED_IN_USER_NAVIDENT = "A321321";
    private static final String OAUTH_VALID_AUDIENCE = "nom-remedy-integrasjon";
    private static final String OAUTH_VALID_ISSUER = "aad";
    private static final String NOM_ADMIN_GROUP = "nom-admin-group";

    @Autowired
    private MockOAuth2Server mockOAuth2Server;

    @Autowired
    protected WebTestClient webTestClient;

    @MockBean
    protected OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    @MockBean
    protected Unleash unleash;

    @MockBean
    protected LeaderElection leaderElection;

    @Autowired
    protected NomIntegrasjonRepository nomIntegrasjonRepository;

    @MockBean
    protected FysiskBrukerstedReadOnlyRepository fysiskBrukerstedReadOnlyRepository;

    protected final String ressursEventSourceTopic = "org.nom.ressurs-eventsource";
    protected final String orgtilknytningEventSourceTopic = "org.nom.orgtilknytning-eventsource";

    @BeforeEach
    void unleashFeaturesEnabled() {
        when(unleash.isEnabled(any())).thenReturn(true);
    }

    protected String noScopesUserToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);
        claims.put("azp_name", "some-app");
        return validToken(claims);
    }

    protected String invalidAudienceToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);
        claims.put("azp_name", "some-app");
        return generateToken(claims, "invalid-audience");
    }

    protected String nomAdminUserToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);
        claims.put("groups", List.of(NOM_ADMIN_GROUP));
        claims.put("azp_name", "some-app");
        return validToken(claims);
    }
    protected String validToken(Map<String, Object> claims) {
        return generateToken(claims, OAUTH_VALID_AUDIENCE);
    }

    protected String generateToken(Map<String, Object>  claims, String audience) {
        return mockOAuth2Server.issueToken(
            OAUTH_VALID_ISSUER,
            "clientId",
            new DefaultOAuth2TokenCallback(
                "aad",
                "subjectId",
                JOSEObjectType.JWT.getType(),
                List.of(audience),
                claims,
                3600
            )
        ).serialize();
    }

}
