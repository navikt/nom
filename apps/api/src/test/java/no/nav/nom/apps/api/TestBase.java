package no.nav.nom.apps.api;

import com.nimbusds.jwt.JWTClaimsSet;
import com.thomaskasene.wiremock.junit.WireMockStubs;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.orgtilknytning.DvhOrgtilknytningMockUtils;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.pdlutils.PdlUtils;
import no.nav.nom.apps.api.postgresql.GenericStorage;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.test.RsaStub;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@WireMockStubs(RsaStub.class)
@SpringBootTest(
    classes = TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"nom-state-v1", "org.nom.ressurs-state-v1", "org.nom.orgenhet-state-v1"}, controlledShutdown = true)
public abstract class TestBase {
    @Autowired
    private SecurityTestUtils securityTestUtils;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Autowired
    private DvhRepository dvhRepository;

    @Autowired
    private GenericStorage genericStorage;

    @Autowired
    public DvhOrgEnhetMockUtils dvhOrgenhetMockUtils;

    @Autowired
    public DvhRessursUtils dvhRessursUtils;

    @Autowired
    protected PdlUtils pdlUtils;

    @Autowired
    protected KafkaUtils kafkaUtils;

    @Autowired
    protected IRessursService ressursService;

    @MockBean
    protected Unleash unleash;

    @Autowired
    protected OrgEnhetStateRepository orgEnhetStateRepository;

    @Autowired
    protected RessursStateRepository ressursStateRepository;

    @Autowired
    protected DvhOrgtilknytningMockUtils dvhOrgtilknytningMockUtils;

    @MockBean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    @BeforeEach
    void reset() {
        dvhRepository.clearRepository();
        orgEnhetStateRepository.clear();
        genericStorage.deleteAll();
        dvhOrgenhetMockUtils.createEmptyResponse();
        dvhOrgtilknytningMockUtils.createEmptyResponse();
        pdlUtils.reset();
        kafkaUtils.clearKafka();
        ressursService.clear();
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

    protected JWTClaimsSet.Builder validJwtWithAzpClaim() {
        final var jwtClaimsBuilder = securityTestUtils.validJwtClaims();
        final var subject = jwtClaimsBuilder.getClaims().get("sub");
        return securityTestUtils.validJwtClaims().claim("azp", subject);
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
            null
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


}
