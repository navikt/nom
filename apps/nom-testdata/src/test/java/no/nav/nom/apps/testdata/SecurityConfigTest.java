package no.nav.nom.apps.testdata;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(SecurityConfigTest.TestConfiguration.class)
class SecurityConfigTest {

    private static final String ISSUER = "https://issuer.example.test/v2.0";
    private static final String AUDIENCE = "testdata-client";
    private static final String ADMIN_GROUP = "9e0e2958-df7a-4376-83f3-9bcbec1f2650";
    private static final RSAKey SIGNING_KEY = signingKey("test-key");
    private static final HttpServer JWKS_SERVER = jwksServer();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> ISSUER);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://127.0.0.1:" + JWKS_SERVER.getAddress().getPort() + "/jwks");
        registry.add("spring.security.oauth2.resourceserver.jwt.audiences", () -> AUDIENCE);
        registry.add("nom.security.admin-group-id", () -> ADMIN_GROUP);
        registry.add("nom.security.pre-authorized-apps", () -> """
                [{"clientId":"admin-web-client","name":"dev-gcp:org:nom-admin-web"},
                 {"clientId":"token-tool-client","name":"dev-gcp:org:org-token-tool"},
                 {"clientId":"generator-client","name":"dev-gcp:nais:azure-token-generator"}]
                """);
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterAll
    static void stopServer() {
        JWKS_SERVER.stop(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin-web-client", "token-tool-client", "generator-client"})
    void acceptsDelegatedAdminTokenFromEachAllowedClient(String client) throws Exception {
        var result = mvc.perform(get("/protected").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + token(builder -> builder.claim("azp", client))))
                .andExpect(status().isOk()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void requiresToken() throws Exception {
        mvc.perform(get("/protected")).andExpect(status().isUnauthorized());
        mvc.perform(get("/protected").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresAdminGroupNotRoles() throws Exception {
        for (Object groups : List.of(List.of("other-group"), "not-a-group-list", List.of())) {
            mvc.perform(get("/protected").header(HttpHeaders.AUTHORIZATION,
                            "Bearer " + token(builder -> builder.claim("groups", groups))))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/protected").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + token(builder -> builder.claim("groups", null)
                                .claim("roles", List.of("NOM_ADMIN", ADMIN_GROUP)))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsWrongOrMissingAudience() throws Exception {
        for (String audience : List.of("nom-admin", "api://" + AUDIENCE)) {
            expectUnauthorized(builder -> builder.audience(audience));
        }
        expectUnauthorized(builder -> builder.audience(List.of()));
    }

    @Test
    void rejectsWrongOrMissingIssuer() throws Exception {
        expectUnauthorized(builder -> builder.issuer("https://other.example.test/v2.0"));
        expectUnauthorized(builder -> builder.issuer(null));
    }

    @Test
    void rejectsInvalidSignature() throws Exception {
        String invalidSignature = token(builder -> {}, signingKey("test-key"));
        mvc.perform(get("/protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidSignature))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresValidExpirationAndNotBefore() throws Exception {
        expectUnauthorized(builder -> builder.expirationTime(Date.from(Instant.now().minusSeconds(120))));
        expectUnauthorized(builder -> builder.expirationTime(null));
        expectUnauthorized(builder -> builder.notBeforeTime(Date.from(Instant.now().plusSeconds(120))));
    }

    @Test
    void rejectsAppOnlyTokenEvenWithAdminGroupAndSpoofedScope() throws Exception {
        expectUnauthorized(builder -> builder.claim("idtyp", "app"));
        expectUnauthorized(builder -> builder.claim("scp", null).claim("roles", List.of("defaultaccess")));
    }

    @Test
    void requiresDelegatedScopeAndUserIdentity() throws Exception {
        expectUnauthorized(builder -> builder.claim("scp", ""));
        expectUnauthorized(builder -> builder.claim("scp", "unrelated-scope"));
        expectUnauthorized(builder -> builder.claim("oid", null));
    }

    @Test
    void rejectsUnknownOrMissingCallingClient() throws Exception {
        expectUnauthorized(builder -> builder.claim("azp", "unknown-client"));
        expectUnauthorized(builder -> builder.claim("azp", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/internal/health/liveness", "/internal/health/readiness", "/internal/prometheus",
            "/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs/swagger-config"})
    void permitsOnlySpecifiedOperationalAndDocumentationRoutes(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/internal/other", "/internal/health", "/internal/health/liveness/other", "/error"})
    void doesNotOpenBroadRoutePrefixes(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    private void expectUnauthorized(Consumer<JWTClaimsSet.Builder> changes) throws Exception {
        mvc.perform(get("/protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(changes)))
                .andExpect(status().isUnauthorized());
    }

    private static String token(Consumer<JWTClaimsSet.Builder> changes) throws JOSEException {
        return token(changes, SIGNING_KEY);
    }

    private static String token(Consumer<JWTClaimsSet.Builder> changes, RSAKey key) throws JOSEException {
        Instant now = Instant.now();
        var builder = new JWTClaimsSet.Builder().issuer(ISSUER).audience(AUDIENCE).subject("synthetic-user")
                .issueTime(Date.from(now.minusSeconds(5))).notBeforeTime(Date.from(now.minusSeconds(5)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("oid", "synthetic-object-id").claim("idtyp", "user")
                .claim("azp", "admin-web-client").claim("scp", "defaultaccess")
                .claim("groups", List.of(ADMIN_GROUP));
        changes.accept(builder);
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(key.getKeyID()).type(JOSEObjectType.JWT).build(), builder.build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static RSAKey signingKey(String keyId) {
        try {
            return new RSAKeyGenerator(2048).keyID(keyId).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not create test signing key", e);
        }
    }

    private static HttpServer jwksServer() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            byte[] body = new JWKSet(SIGNING_KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            server.createContext("/jwks", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var response = exchange.getResponseBody()) {
                    response.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("Could not start test JWKS server", e);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @EnableConfigurationProperties(OAuth2ResourceServerProperties.class)
    @Import({SecurityConfig.class, TestController.class})
    static class TestConfiguration {
    }

    @RestController
    static class TestController {
        @GetMapping({"/protected", "/internal/health/liveness", "/internal/health/readiness", "/internal/prometheus",
                "/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs/swagger-config",
                "/internal/other", "/internal/health", "/internal/health/liveness/other", "/error"})
        String response() {
            return "ok";
        }
    }
}
