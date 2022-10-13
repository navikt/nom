package no.nav.nom.apps.api;

import com.nimbusds.jwt.JWTClaimsSet;
import com.thomaskasene.wiremock.junit.WireMockStubs;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.search.SearchResolver;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhDataLoader;
import no.nav.nom.apps.api.pdlutils.PdlUtils;
import no.nav.nom.apps.api.postgresql.GenericStorage;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.test.RsaStub;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.StreamUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@ActiveProfiles({
        "test",
        "log-console-error"
})
@WireMockStubs(RsaStub.class)
@SpringBootTest(
        classes = TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(
        initializers = TestBase.Initializer.class
)
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"nom-state-v1"}, controlledShutdown = true)
public abstract class TestBase {

    // Singleton pattern with static start and custom spring initializer
    public static PostgreSQLContainer<?> postgreSQLContainer = CustomPostgreSQLContainer.getInstance();

    static {
        postgreSQLContainer.start();
    }

    @Autowired
    private SecurityTestUtils securityTestUtils;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Autowired
    private DvhRepository dvhRepository;

    @Autowired
    private GenericStorage genericStorage;

    @Autowired
    public DvhOrgenhetMockUtils dvhOrgenhetMockUtils;

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

    @BeforeEach
    void reset() {
        dvhRepository.clearRepository();
        genericStorage.deleteAll();
        dvhOrgenhetMockUtils.createEmptyResponse();
        pdlUtils.reset();
        kafkaUtils.clearKafka();
        ressursService.clear();
        dvhRessursUtils.createEmptyResponse();
        luceneEnabled(false);
        enableOrgEnhetEventSource(true);
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                    "spring.datasource.password=" + postgreSQLContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
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

    protected void createRessursHendelseEventSource(RessursState ressursState) {
        kafkaUtils.send(ressursState.fid(), ressursState);
    }

    protected void luceneEnabled(boolean toggle) {
        Mockito.when(unleash.isEnabled(SearchResolver.UNLEASH_LUCENE_TOGGLE)).thenReturn(toggle);
    }

    protected void enableOrgEnhetEventSource(boolean toggle) {
        Mockito.when(unleash.isEnabled(DvhDataLoader.UNLEASH_TOGGLE)).thenReturn(toggle);
    }

}
