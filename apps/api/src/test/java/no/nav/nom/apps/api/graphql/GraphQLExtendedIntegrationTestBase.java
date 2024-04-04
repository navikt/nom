package no.nav.nom.apps.api.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.getunleash.Unleash;
import lombok.SneakyThrows;
import no.nav.nom.apps.api.KafkaUtils;
import no.nav.nom.apps.api.MockOAuth2ServerInitializer;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.controller.GraphQLOrgEnhetController;
import no.nav.nom.apps.api.graphql.controller.GraphQLPersonController;
import no.nav.nom.apps.api.graphql.controller.GraphQLRessursController;
import no.nav.nom.apps.api.graphql.controller.GraphQLSearchController;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.pdlutils.PdlUtils;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyMap;

@ActiveProfiles("test")
@ContextConfiguration(classes = { GraphqlTestConfig.class, GraphQLExtendedIntegrationTestConfig.class },  initializers = MockOAuth2ServerInitializer.class)
@EmbeddedKafka(partitions = 1,  topics = {"org.nom.intern-ressurs-state", "org.nom.intern-orgenhet-state"},  controlledShutdown = true)
@AutoConfigureWireMock(port = 0)
@GraphQlTest(controllers = {GraphQLRessursController.class, GraphQLPersonController.class, GraphQLOrgEnhetController.class, GraphQLSearchController.class})
public class GraphQLExtendedIntegrationTestBase {

    private final static Duration TIMEOUT_DURATION = Duration.of(5, SECONDS);
    private final ThreadLocal<Duration> responseTimeout = ThreadLocal.withInitial(() -> TIMEOUT_DURATION);

    @Autowired
    ExecutionGraphQlServiceTester executionGraphQlServiceTester;

    @Autowired
    public ObjectMapper objectMapper;

    @Autowired
    DvhStartupLoadMonitor dvhStartupLoadMonitor;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Autowired
    protected TestTokensGenerator testToken;

    @Autowired
    protected KafkaUtils kafkaUtils;

    @Autowired
    protected PdlUtils pdlUtils;

    @Autowired
    protected Unleash unleash;

    @Autowired
    protected DvhRepository dvhRepository;

    @Autowired
    protected OrgEnhetStateRepository orgEnhetStateRepository;

    @Autowired
    protected OrganiseringStateRepository organiseringStateRepository;

    @Autowired
    RessursStateRepository ressursStateRepository;

    @BeforeEach
    protected void setup() {
        dvhStartupLoadMonitor.setCompleted();
        pdlUtils.reset();
        kafkaUtils.clearKafka();

        dvhRepository.clearRepository();
        orgEnhetStateRepository.clear();
        organiseringStateRepository.clear();
        ressursStateRepository.clear();
    }

    @SneakyThrows
    protected GraphQlTester.Response postForResource(String resource) {
        return postForResource(resource, emptyMap());
    }

    @SneakyThrows
    public GraphQlTester.Response postForResource(String resource, String token, Map<String, Object> vars) {
        var query = executionGraphQlServiceTester
            .mutate().responseTimeout(responseTimeout.get())
            .build()
            .document(loadResourceFromClasspath(resource)); // Load the resource
        vars.forEach(query::variable); // Load variables into the query

        return query.execute();
    }

    @SneakyThrows
    protected GraphQlTester.Response postForResource(String resource, String token) {
        return postForResource(resource, token, emptyMap());
    }

    @SneakyThrows
    protected GraphQlTester.Response postForResource(String resource, Map<String, Object> vars) {
        return postForResource(resource, testToken.nomAdminUser(), vars);
    }

    @SneakyThrows
    protected GraphQlTester.Response postForResourceWithoutAzp(String resource) {
        return postForResource(resource, testToken.noScopesUser(), emptyMap());
    }

    @BeforeEach
    protected void resetClientResponseTimeout() {
        this.responseTimeout.set(TIMEOUT_DURATION);
    }

    protected String loadResourceFromClasspath(String location) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + location);
        return loadResource(resource);
    }

    private String loadResource(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }
}
