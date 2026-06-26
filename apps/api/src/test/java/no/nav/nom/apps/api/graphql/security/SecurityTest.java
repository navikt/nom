package no.nav.nom.apps.api.graphql.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.AZPAuthoritiesConverter;
import no.nav.nom.apps.api.common.security.AppGrantedAuthoritiesProperties;
import no.nav.nom.apps.api.common.security.JWTFilter;
import no.nav.nom.apps.api.common.security.SecurityConfig;
import no.nav.nom.apps.api.graphql.GraphQlStateReposConfig;
import no.nav.nom.apps.api.graphql.NomGraphQlControllerImpl;
import no.nav.nom.apps.api.graphql.NomGraphQlSupportImpl;
import no.nav.nom.apps.api.graphql.config.ExceptionhandlerConfig;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.gql.NomGraphQlAutoConfiguration;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration;
import org.springframework.boot.graphql.autoconfigure.servlet.GraphQlWebMvcAutoConfiguration;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

@Slf4j
@ActiveProfiles({"GraphQLTest", "test","localgql"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureHttpGraphQlTester
@EnableConfigurationProperties(AppProperties.class)
@ContextConfiguration(
    initializers = {MockOAuth2ServerInitializer.class},
    classes = {
        NomGraphQlAutoConfiguration.class
        , GraphQlStateReposConfig.class
        , NomGraphQlSupportImpl.class
        , GraphQlAutoConfiguration.class
        , NomGraphQlControllerImpl.class
        , GraphQlWebMvcAutoConfiguration.class
        , SecurityConfig.class
        , OAuth2ResourceServerAutoConfiguration.class
        , TestTokensGenerator.class
        , AuthResolver.class
        , JWTFilter.class
        , AppGrantedAuthoritiesProperties.class
        , AZPAuthoritiesConverter.class
        , WebMvcAutoConfiguration.class
        , ExceptionhandlerConfig.class
    }
)
class SecurityTest {

    private final static Duration TIMEOUT_DURATION = Duration.of(5, SECONDS);

    private final ThreadLocal<Duration> responseTimeout = ThreadLocal.withInitial(() -> TIMEOUT_DURATION);

    @Autowired
    private HttpGraphQlTester httpGraphQlTester;

    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    MockOAuth2Server mockOAuth2Server;

    @Autowired
    TestTokensGenerator testTokensGenerator;

    @Autowired
    OrgEnhetStateRepository orgEnhetStateRepository;


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
    public GraphQlTester.Response postQuery(String queryStr, String token, Map<String, Object> vars) {
        final var client = httpGraphQlTester.mutate()
            .webTestClient(builder -> builder.responseTimeout(responseTimeout.get()))
            .headers(httpHeaders -> httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .build(); // Build the correct client

        var query = client.document(queryStr); // Load the resource

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


    private void postAndAssertResponse(String resource, String token, String path) {
        var response = postForResource(resource, token);

        response.errors().expect(responseError -> "Unauthorized".equals(responseError.getMessage()));
        response.path(requireNonNullElse(path, "")).valueIsNull();
        //response.assertThatListOfErrors().hasSize(1);
    }


    private static final String orgenhetResource = "organisasjonsenhet/scenario1.graphql";
    private static final String orgenheterResource = "organisasjonsenhet/scenario2.graphql";
    private static final String ressursResource = "ressurs/scenario1.graphql";
    private static final String ressurserResource = "ressurs/scenario3.graphql";

    private void testToken(String token) {
        // if response is array, data is null, if it is an object data contains the field which is null for some reason;
        postAndAssertResponse(orgenhetResource, token, "organisasjonsenhet");
        postAndAssertResponse(orgenheterResource, token, null);
        postAndAssertResponse(ressursResource, token, "ressurs");
        postAndAssertResponse(ressurserResource, token, null);
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


    @MockitoBean
    AuthorizedNavident authorizedNavident;

    @Autowired
    AuthResolver authResolver;

    @Autowired
    TestTokensGenerator testToken;


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

    @Test
    @DisplayName("Gitt gyldig token, får man hentet ut data")
    public void scenario30(ApplicationContext applicationContext) {
        orgEnhetStateRepository.update(OrgEnhetState.builder().id("abc").fid("fid-abc").navn("abc-enheten").build());

        // language=graphql
        var queryStr = """
                       query {
                           organisasjonsenhet(where: {id: "abc"}) {
                               navn
                           }
                       }
                       """;

        var response = postQuery(queryStr, validClaimsToken(), Map.of());
        response.path("organisasjonsenhet.navn").hasValue().matchesJson("\"abc-enheten\"");
    }


    private String validClaimsToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);
        return testTokensGenerator.validToken(claims);
    }


}

