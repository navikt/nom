package no.nav.nom.apps.api.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadata;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;
import no.nav.nom.apps.api.ressurs.UnleashRessursServiceSelector;
import no.nav.nom.apps.api.ressurs.contract.IRessurs;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyMap;

@AutoConfigureGraphQlTester
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public abstract class GraphQLTestBase extends TestBase {

    private final ThreadLocal<Duration> responseTimeout = ThreadLocal.withInitial(() -> Duration.of(5, SECONDS));

    @Autowired
    HttpGraphQlTester graphQlTester;

    @Autowired
    private DvhRepository dvhRepository;

    @Autowired
    protected SecurityTestUtils securityTestUtils;

    @Autowired
    protected ObjectMapper objectMapper;

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
        return postForResource(resource, createJwtForTest(true), vars);
    }

    @SneakyThrows
    protected GraphQlTester.Response postForResourceWithoutAzp(String resource) {
        return postForResource(resource, createJwtForTest(false), emptyMap());
    }

    @BeforeEach
    protected void resetClientResponseTimeout() {
        this.responseTimeout.set(Duration.of(5, SECONDS));
    }

    protected void setClientResponseTimeout(Duration duration) {
        this.responseTimeout.set(duration);
    }

    private String createJwtForTest(boolean includeValidAzp) {
        final var claims = includeValidAzp ? validJwtWithAzpClaim() : securityTestUtils.validJwtClaims();
        claims.claim("azp_name", "test");
        claims.claim("NAVident", "A321321");

        return securityTestUtils.createToken(claims);
    }

    protected IRessurs createRessursHendelse(String suffix) {
        final var metadata = HendelseMetadataSerialize.builder()
                .opprettetAv(new HendelseMetadata.OpprettetAv("test", "test"))
                .rawInputData(objectMapper.createObjectNode().put("suffix", suffix))
                .opprettetTidspunkt(LocalDateTime.now())
                .build();
        return ressursService.opprettRessurs("testPersonIdent" + suffix, "testNavIdent" + suffix, null, metadata);
    }
}
