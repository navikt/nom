package no.nav.nom.apps.api.graphql;

import lombok.SneakyThrows;
import no.nav.nom.apps.api.TestTokensGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyMap;

@Component
@Profile("GraphQLTest")
public class GraphQLTestUtil {
    private final static Duration TIMEOUT_DURATION = Duration.of(5, SECONDS);
    private final ThreadLocal<Duration> responseTimeout = ThreadLocal.withInitial(() -> TIMEOUT_DURATION);

    @Autowired
    ExecutionGraphQlServiceTester executionGraphQlServiceTester;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Autowired
    protected TestTokensGenerator testToken;

    @SneakyThrows
    public GraphQlTester.Response postForResource(String resource) {
        return postForResource(resource, emptyMap());
    }

    @SneakyThrows
    public GraphQlTester.Response postForResource(String resource, Map<String, Object> vars) {
        return postForResource(resource, testToken.nomAdminUser(), vars);
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
