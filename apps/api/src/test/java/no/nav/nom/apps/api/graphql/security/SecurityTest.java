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
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNullElse;

@DisplayName("GraphQL.Sikkerhet")
class SecurityTest extends SecurityTestBase {

    private static final String organiseringResource = "organisasjonsenhet/scenario1.graphql";
    private static final String organiseringerResource = "organisasjonsenhet/scenario2.graphql";
    private static final String ressursResource = "ressurs/scenario1.graphql";
    private static final String ressurserResource = "ressurs/scenario3.graphql";


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

        testToken(generateToken(invalidIssuerValue, claims, OAUTH_VALID_AUDIENCE));
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt ugyldig audience i token, får man Unauthenticated error i responsen")
    public void scenario25() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", LOGGED_IN_USER_NAVIDENT);

        var invalidAudienceValue = "invalidAudience";

        testToken(generateToken(OAUTH_VALID_ISSUER, claims, invalidAudienceValue));
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
        val futureNbfToken = validToken(claims);

        testToken(futureNbfToken);
    }

    private void testToken(String token) {
        // if response is array, data is null, if it is an object data contains the field which is null for some reason
        postAndAssertResponse(organiseringResource, token, "organisasjonsenhet");
        postAndAssertResponse(organiseringerResource, token, null);
        postAndAssertResponse(ressursResource, token, "ressurs");
        postAndAssertResponse(ressurserResource, token, null);
    }

    @SneakyThrows
    private void postAndAssertResponse(String resource, String token, String path) {
        var response = postForResource(resource, token);

        response.errors().expect(responseError -> "Unauthorized".equals(responseError.getMessage()));
        response.path(requireNonNullElse(path, "")).valueIsNull();
        //response.assertThatListOfErrors().hasSize(1);
    }

}
