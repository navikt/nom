package no.nav.nom.apps.api;

import com.nimbusds.jose.JOSEObjectType;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.organisering.DvhOrganiseringMockUtils;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateRepository;
import no.nav.nom.apps.api.pdlutils.PdlUtils;
import no.nav.nom.apps.api.postgresql.GenericStorage;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.StreamUtils;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(
    classes = TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"nom-state-v1", "org.nom.ressurs-state-v1", "org.nom.orgenhet-state-v1", "org.nom.nom-mastered-ressurs", "org.nom.organisering-eventsource"}, controlledShutdown = true)
@ContextConfiguration(initializers = {MockOAuth2ServerInitializer.class})
@AutoConfigureWireMock(port = 0)
public abstract class TestBase {

    @Autowired
    protected MockOAuth2Server mockOAuth2Server;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Autowired
    private DvhRepository dvhRepository;

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

    @MockBean
    protected Unleash unleash;

    @Autowired
    protected OrgEnhetStateRepository orgEnhetStateRepository;

    @Autowired
    protected OrganiseringStateRepository organiseringStateRepository;

    @Autowired
    protected OrgtilknytningStateRepository orgtilknytningStateRepository;

    @MockBean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    public static String LOGGED_IN_USER_NAVIDENT = "A321321";
    public static String OAUTH_VALID_AUDIENCE = "test";
    public static String OAUTH_VALID_ISSUER = "aad";

    @BeforeEach
    void reset() {
        dvhRepository.clearRepository();
        orgEnhetStateRepository.clear();
        organiseringStateRepository.clear();
        genericStorage.deleteAll();
        dvhOrgenhetMockUtils.createEmptyResponse();
        pdlUtils.reset();
        kafkaUtils.clearKafka();
        ressursStateRepository.clear();
        dvhRessursUtils.createEmptyResponse();
    }

    @BeforeEach
    void mockOAuth2AuthorizedClientManager() {
        OAuth2AuthorizedClient oAuth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);
        when(oAuth2AuthorizedClientManager.authorize(any())).thenReturn(oAuth2AuthorizedClient);
        when(oAuth2AuthorizedClient.getAccessToken()).thenReturn(
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

    protected RessursState createRessursHendelseEventSource(String suffix) {
        final var ressursState = new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            "testPersonIdent" + suffix,
            "testNavIdent" + suffix,
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "fornavn" + suffix,
                Optional.of("mellomnavn" + suffix),
                "etternavn" + suffix
            ),
            null,
                Optional.of(LocalDate.now()),
            null,
            Collections.emptyList()
        );

        createRessursHendelseEventSource(ressursState);

        return ressursState;
    }

    protected OrgEnhetState createOrgenhetHendelseEventSource(String suffix) {
        final var orgEnhetState = new OrgEnhetState(
            PrefixedUlidCreator.createOrgEnhetUlid(),
            "testOrgenhetId" + suffix,
            "testOrgenhetNavn" + suffix,
            LocalDate.now(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of("0000000".substring(0, 7 - suffix.length()) + suffix),
            Optional.of("ORGENHET"),
            Optional.empty()
        );

        createOrgenhetHendelseEventSource(orgEnhetState);

        return orgEnhetState;
    }

    protected void createRessursHendelseEventSource(RessursState ressursState) {
        kafkaUtils.sendRessurs(ressursState.fid(), ressursState);
    }

    protected void createOrgenhetHendelseEventSource(OrgEnhetState orgEnhetState) {
        kafkaUtils.sendOrgenhet(orgEnhetState.fid(), orgEnhetState);
    }

    protected void saveOrgtilknytninger(List<OrgtilknytningState> orgtilknytningStateList) {
        orgtilknytningStateList.forEach(o -> orgtilknytningStateRepository.putEventAggregate(o.fid(), o));
    }

    protected void saveOrgenheter(List<OrgEnhetState> orgEnhetStateList) {
        orgEnhetStateList.forEach(o -> orgEnhetStateRepository.putEventAggregate(o));
    }

    protected void clearOrgtilknytninger() {
        orgtilknytningStateRepository.getAll()
            .forEach(o -> orgtilknytningStateRepository.putEventAggregate(o.fid(), null));
    }

    protected void saveRessurser(List<RessursState> ressursStateList) {
        ressursStateList.forEach(r -> ressursStateRepository.putEventAggregate(r));
    }

    protected String generateToken(String issuer, Map<String, Object>  claims, String audience) {
        return mockOAuth2Server.issueToken(
            issuer,
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

    protected String validToken(Map<String, Object> claims) {
        return generateToken(OAUTH_VALID_ISSUER, claims, OAUTH_VALID_AUDIENCE);
    }



    protected String nomAdminUserToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);
        claims.put("groups", List.of("nomAdminGroup"));
        claims.put("azp_name", "some-app");
        return validToken(claims);
    }

    protected String nomWriteGroupAndNomUiAzpToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);
        claims.put("groups", List.of("nomWriteGroup"));
        claims.put("azp_name", "nomUiClient");
        return validToken(claims);
    }

    protected String noScopesUserToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);
        claims.put("azp_name", "some-app");
        return validToken(claims);
    }

    protected String queryPersonClientToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("azp_name", "queryPersonClient");
        return validToken(claims);
    }

    protected String mutateRessursClientToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("azp_name", "mutateRessursClient");
        return validToken(claims);
    }

    protected String noScopesClientToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("azp_name", "noScopesClient");
        return validToken(claims);
    }

}
