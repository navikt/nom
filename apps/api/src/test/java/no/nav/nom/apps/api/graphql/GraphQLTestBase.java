package no.nav.nom.apps.api.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadata;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;
import no.nav.nom.apps.api.ressurs.contract.IRessurs;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyMap;
import static no.nav.nom.apps.api.graphql.GraphQLMatchers.hasErrors;
import static no.nav.nom.apps.api.graphql.GraphQLMatchers.hasNoErrors;
import static org.hamcrest.MatcherAssert.assertThat;

@AutoConfigureGraphQlTester // Me thinks this is right
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public abstract class GraphQLTestBase extends TestBase {

    private ThreadLocal<Duration> responseTimeout = ThreadLocal.withInitial(() -> Duration.of(5, SECONDS));

    @Autowired
    HttpGraphQlTester graphQlTester;

    @Autowired
    private DvhRepository dvhRepository;

    @Autowired
    protected SecurityTestUtils securityTestUtils;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected IRessursService ressursService;

    @SneakyThrows
    protected GraphQlTester.Response postForResource(String resource, boolean hasErrors) {
        return postForResource(resource, hasErrors, emptyMap());
    }

    @SneakyThrows
    private GraphQlTester.Response postForResource(String resource, boolean hasErrors, Map<String, Object> vars, boolean includeValidAzp) {
        final var claims = includeValidAzp ? validJwtWithAzpClaim() : securityTestUtils.validJwtClaims();
        claims.claim("azp_name", "test");
        claims.claim("NAVident", "A321321");
        final var token = securityTestUtils.createToken(claims);

        final var client = graphQlTester.mutate()
                .webTestClient(builder -> builder.responseTimeout(responseTimeout.get()))
                .headers(httpHeaders -> httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .build(); // Build the correct client

        var query = client.document(loadResourceFromClasspath(resource)); // Load the resource
        vars.forEach(query::variable); // Load variables into the query

        final var response = query.execute();
        assertThat(response, hasErrors ? hasErrors() : hasNoErrors());

        return response;
    }

    @SneakyThrows
    protected GraphQlTester.Response postForResource(String resource, boolean hasErrors, Map<String, Object> vars) {
        return postForResource(resource, hasErrors, vars, true);
    }

    @SneakyThrows
    protected GraphQlTester.Response postForResourceWithoutAzp(String resource, boolean hasErrors) {
        return postForResource(resource, hasErrors, emptyMap(), false);
    }

    @BeforeEach
    protected void resetClientResponseTimeout() {
        this.responseTimeout.set(Duration.of(5, SECONDS));
    }

    protected void setClientResponseTimeout(Duration duration) {
        this.responseTimeout.set(duration);
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
