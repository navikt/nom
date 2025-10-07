package no.nav.nom.apps.api.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.getunleash.Unleash;
import lombok.SneakyThrows;
import no.nav.nom.apps.api.KafkaUtils;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.controller.GraphQLOrgEnhetController;
import no.nav.nom.apps.api.graphql.controller.GraphQLRessursController;
import no.nav.nom.apps.api.graphql.controller.GraphQLSearchController;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.orgtilknytning.MasseflyttOrgtilknytningerService;
import no.nav.nom.apps.api.pdlutils.PdlUtils;
import no.nav.nom.apps.api.ressurs.eventsourcing.recoverybackup.RessursEventSourceRepository;
import no.nav.nom.apps.api.telefon.TelefonService;
import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.organisering.OrganiseringStateAutoconfiguration;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateAutoconfiguration;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.NomStateOrgEnhetLederListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateAutoconfiguration;
import no.nav.nom.libs.state.ressurs.RessursStateAutoconfiguration;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateAutoconfiguration;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyMap;

@ActiveProfiles("test")
@ContextConfiguration(
    classes = {
        GraphqlTestConfig.class,
        RessursStateAutoconfiguration.class,
        OrgEnhetStateAutoconfiguration.class,
        OrganiseringStateAutoconfiguration.class,
        OrgtilknytningStateAutoconfiguration.class,
        MasteredRessursStateAutoconfiguration.class
    },
    initializers = MockOAuth2ServerInitializer.class)
@EmbeddedKafka(partitions = 1,  topics = {"org.nom.intern-ressurs-state", "org.nom.intern-orgenhet-state", "org.nom.intern-organisering-state", "org.nom.intern-orgtilknytning-state"},  controlledShutdown = true)
@AutoConfigureWireMock(port = 0)
@GraphQlTest(
    properties = {
        "nom.libs.state.idle-event-interval-seconds: 1",
        "nom.libs.state.ressurs.enabled: true",
        "nom.libs.state.ressurs.state-topic: org.nom.intern-ressurs-state",
        "nom.libs.state.ressurs.listener-id: nomStateRessursListener_test",
        "nom.libs.state.orgenhet.enabled: true",
        "nom.libs.state.orgenhet.state-topic: org.nom.intern-orgenhet-state",
        "nom.libs.state.orgenhet.listener-id: nomStateOrgenhetListener_test",
        "nom.libs.state.organisering.enabled: true",
        "nom.libs.state.organisering.state-topic: org.nom.intern-organisering-state",
        "nom.libs.state.organisering.listener-id: nomStateOrganiseringListener_test",
        "nom.libs.state.orgtilknytning.enabled: true",
        "nom.libs.state.orgtilknytning.state-topic: org.nom.intern-organisering-state",
        "nom.libs.state.orgtilknytning.listener-id: nomStateOrgtilknytningListener_test",
        "nom.libs.state.mastered-ressurs.enabled: true",
        "nom.libs.state.mastered-ressurs.state-topic: org.nom.nom-mastered-ressurs",
        "nom.libs.state.mastered-ressurs.listener-id: nomStateMasteredRessursListener_test",
        "nom.libs.state.orgenhet-leder.listener-id: nomStateOrgEnhetLederListener_test"
    }
    , controllers = {GraphQLRessursController.class, GraphQLOrgEnhetController.class, GraphQLSearchController.class})
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

    @MockitoBean
    MasseflyttOrgtilknytningerService masseflyttOrgtilknytningerService;

    @MockitoBean
    TelefonService telefonService;

    @MockitoBean
    OrgEnhetCommandService orgEnhetCommandService;

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

    @MockitoBean
    RessursEventSourceRepository ressursEventSourceRepository;

    @MockitoBean
    AuthorizedNavident authorizedNavident;

    @MockitoBean
    GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter;

    @MockitoBean
    OrgEnhetLederService orgEnhetLederService;

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
