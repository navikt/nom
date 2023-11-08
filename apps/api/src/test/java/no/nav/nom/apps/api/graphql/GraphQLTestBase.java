package no.nav.nom.apps.api.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.util.Map;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyMap;

@AutoConfigureGraphQlTester
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public abstract class GraphQLTestBase extends TestBase {

    private final static Duration TIMEOUT_DURATION = Duration.of(5, SECONDS);

    private final ThreadLocal<Duration> responseTimeout = ThreadLocal.withInitial(() -> TIMEOUT_DURATION);

    @Autowired
    HttpGraphQlTester graphQlTester;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    DvhStartupLoadMonitor dvhStartupLoadMonitor;

    @BeforeEach
    protected void setup() {
        dvhStartupLoadMonitor.setCompleted();
    }

    @SneakyThrows
    protected GraphQlTester.Response postForResource(String resource) {
        return postForResource(resource, emptyMap());
    }

    @SneakyThrows
    public GraphQlTester.Response postForResource(String resource, String token, Map<String, Object> vars) {
        final var client = graphQlTester.mutate()
                .webTestClient(builder -> builder.responseTimeout(responseTimeout.get()))
                .headers(httpHeaders -> httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .build(); // Build the correct client

        var query = client.document(loadResourceFromClasspath(resource)); // Load the resource
        vars.forEach(query::variable); // Load variables into the query

        return query.execute();
    }

    @SneakyThrows
    protected GraphQlTester.Response postForResource(String resource, String token) {
        return postForResource(resource, token, emptyMap());
    }

    @SneakyThrows
    protected GraphQlTester.Response postForResource(String resource, Map<String, Object> vars) {
        return postForResource(resource, nomAdminUserToken(), vars);
    }

    @SneakyThrows
    protected GraphQlTester.Response postForResourceWithoutAzp(String resource) {
        return postForResource(resource, noScopesUserToken(), emptyMap());
    }

    @BeforeEach
    protected void resetClientResponseTimeout() {
        this.responseTimeout.set(TIMEOUT_DURATION);
    }
}
