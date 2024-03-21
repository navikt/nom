package no.nav.nom.apps.api.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyMap;

@AutoConfigureGraphQlTester
@ActiveProfiles("test")
@SpringBootTest(
    classes = TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public abstract class GraphQLTestBase  {

    private final static Duration TIMEOUT_DURATION = Duration.of(5, SECONDS);

    private final ThreadLocal<Duration> responseTimeout = ThreadLocal.withInitial(() -> TIMEOUT_DURATION);

    @Autowired
    HttpGraphQlTester graphQlTester;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    DvhStartupLoadMonitor dvhStartupLoadMonitor;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Autowired
    TestTokensGenerator testToken;

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
