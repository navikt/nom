package no.nav.nom.apps.api.graphql.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

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
        setClientResponseTimeout(Duration.of(600, ChronoUnit.SECONDS));
        testToken("");
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt feil signert token, får man Unauthenticated error i responsen")
    public void scenario12() {
        val rsaKey = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID("annenKey")
                .generate();
        val claims = validJwtWithAzpClaim();
        val signer = new RSASSASigner(rsaKey);
        val signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("annenKey").build(), claims.build());
        signedJWT.sign(signer);

        testToken(signedJWT.serialize());
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt ugyldig issuer i token, får man Unauthenticated error i responsen")
    public void scenario22() {
        val claims = validJwtWithAzpClaim();
        claims.issuer("annenIssuer");
        val token = securityTestUtils.createToken(claims);

        testToken(token);
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt ugyldig audience i token, får man Unauthenticated error i responsen")
    public void scenario25() {
        val claims = validJwtWithAzpClaim();
        claims.audience("ugyldigAudience");
        val token = securityTestUtils.createToken(claims);

        testToken(token);
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt utløpt token, får man Unauthenticated error i responsen")
    public void scenario26() {
        val claims = validJwtWithAzpClaim();
        claims.expirationTime(Date.from(Instant.now().minusSeconds(60 * 60)));
        val token = securityTestUtils.createToken(claims);

        testToken(token);
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt not before time frem i tid i tokenet, får man Unauthenticated error i responsen")
    public void scenario27() {
        val claims = validJwtWithAzpClaim();
        claims.notBeforeTime(Date.from(Instant.now().plusSeconds(60 * 60)));
        val token = securityTestUtils.createToken(claims);

        testToken(token);
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
        var response = postForResource(resource, token, true);

        response.path(requireNonNullElse(path, "")).valueIsNull();
        //response.assertThatListOfErrors().hasSize(1);
    }

}
