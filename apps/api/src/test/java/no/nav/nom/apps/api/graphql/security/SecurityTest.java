package no.nav.nom.apps.api.graphql.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.graphql.GraphQlTestBase;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNullElse;
import static no.nav.nom.apps.api.TestTokensGenerator.LOGGED_IN_USER_NAVIDENT;
import static no.nav.nom.apps.api.TestTokensGenerator.OAUTH_VALID_AUDIENCE;
import static no.nav.nom.apps.api.TestTokensGenerator.OAUTH_VALID_ISSUER;

// TODO - Går det an å få lastet kun påkrevd context og ikke hele applikasjonen her for å få testene til å gå raskere?

//@WebMvcTest
@GraphQlTest
@DisplayName("GraphQL.Sikkerhet")
@Import({SecurityAutoConfiguration.class, TestTokensGenerator.class })
//@ContextConfiguration(classes = SecurityTest.MockOauth2Helper.class)
    @ContextConfiguration(classes = {}, initializers = MockOAuth2ServerInitializer.class)
class SecurityTest extends GraphQlTestBase {

    private final static Duration TIMEOUT_DURATION = Duration.of(5, SECONDS);

    private final ThreadLocal<Duration> responseTimeout = ThreadLocal.withInitial(() -> TIMEOUT_DURATION);


    private final HttpGraphQlTester httpGraphQlTester;
    private final ResourceLoader resourceLoader;
    private final TestTokensGenerator testToken;

    public SecurityTest(MockOAuth2Server mockOAuth2Server, HttpGraphQlTester httpGraphQlTester, ResourceLoader resourceLoader, TestTokensGenerator testToken) {
        this.mockOAuth2Server = mockOAuth2Server;
        this.httpGraphQlTester = httpGraphQlTester;
        this.resourceLoader = resourceLoader;
        this.testToken = testToken;
    }

    MockOAuth2Server mockOAuth2Server;

    private static final String organiseringResource = "organisasjonsenhet/scenario1.graphql";
    private static final String organiseringerResource = "organisasjonsenhet/scenario2.graphql";
    private static final String ressursResource = "ressurs/scenario1.graphql";
    private static final String ressurserResource = "ressurs/scenario3.graphql";

    @Autowired
    TestTokensGenerator testTokensGenerator;

    @Test
    @SneakyThrows
    @DisplayName("Gitt manglende token, får man Unauthenticated error i responsen")
    public void scenario11() {
        testToken("");
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt feil signert token, får man Unauthenticated error i responsen")
    public void scenario12() {
        var validToken = mockOAuth2Server.issueToken(
            OAUTH_VALID_ISSUER,
            "clientId",
            new DefaultOAuth2TokenCallback(
                "aad",
                "subjectId",
                JOSEObjectType.JWT.getType(),
                List.of(OAUTH_VALID_AUDIENCE),
                Map.of("NAVident", LOGGED_IN_USER_NAVIDENT),
                0
            )
        );

        val rsaKey = new RSAKeyGenerator(2048).keyUse(KeyUse.SIGNATURE).keyID("annenKey").generate();
        val claims = validToken.getJWTClaimsSet();
        val signer = new RSASSASigner(rsaKey);
        val signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("annenKey").build(), claims);
        signedJWT.sign(signer);

        var invalidSignatureToken = signedJWT.serialize();

        testToken(invalidSignatureToken);
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt ugyldig issuer i token, får man Unauthenticated error i responsen")
    public void scenario22() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);

        var invalidIssuerValue = "invalidIssuer";

        testToken(testTokensGenerator.generateToken(invalidIssuerValue, claims, OAUTH_VALID_AUDIENCE));
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt ugyldig audience i token, får man Unauthenticated error i responsen")
    public void scenario25() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);

        var invalidAudienceValue = "invalidAudience";

        testToken(testTokensGenerator.generateToken(OAUTH_VALID_ISSUER, claims, invalidAudienceValue));
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt utløpt token, får man Unauthenticated error i responsen")
    public void scenario26() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);

        var expiredToken = mockOAuth2Server.issueToken(
            OAUTH_VALID_ISSUER,
            "clientId",
            new DefaultOAuth2TokenCallback(
                "aad",
                "subjectId",
                JOSEObjectType.JWT.getType(),
                List.of(OAUTH_VALID_AUDIENCE),
                claims,
                0
            )
        ).serialize();

        testToken(expiredToken);
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt not before time frem i tid i tokenet, får man Unauthenticated error i responsen")
    public void scenario27() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);
        claims.put("nbf", Date.from(Instant.now().plusSeconds(60 * 60)));
        val futureNbfToken = testTokensGenerator.validToken(claims);

        testToken(futureNbfToken);
    }

    private void testToken(String token) {
        // if response is array, data is null, if it is an object data contains the field which is null for some reason
        postAndAssertResponse(organiseringResource, token, "organisasjonsenhet");
        postAndAssertResponse(organiseringerResource, token, null);
        postAndAssertResponse(ressursResource, token, "ressurs");
        postAndAssertResponse(ressurserResource, token, null);
    }

    private void postAndAssertResponse(String resource, String token, String path) {
        var response = postForResource(resource, token);

        response.errors().expect(responseError -> "Unauthorized".equals(responseError.getMessage()));
        response.path(requireNonNullElse(path, "")).valueIsNull();
        //response.assertThatListOfErrors().hasSize(1);
    }


    @SneakyThrows
    protected GraphQlTester.Response postForResource(String resource) {
        return postForResource(resource, emptyMap());
    }

    @SneakyThrows
    public GraphQlTester.Response postForResource(String resource, String token, Map<String, Object> vars) {
        final var client = httpGraphQlTester.mutate()
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

}
