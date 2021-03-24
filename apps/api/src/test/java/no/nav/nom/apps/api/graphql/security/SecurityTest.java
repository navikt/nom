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

import java.time.Instant;
import java.util.Date;

import static no.nav.nom.apps.api.graphql.GraphQLMatchers.hasErrors;
import static org.hamcrest.MatcherAssert.assertThat;

@DisplayName("GraphQL.Sikkerhet")
class SecurityTest extends SecurityTestBase {

    private static final String organiseringResource = "organisasjonsenhet/scenario41.graphql";
    private static final String ressursResource = "ressurs/scenario42.graphql";


    @Test
    @SneakyThrows
    @DisplayName("Gitt manglende token, får man Unauthenticated error i responsen")
    public void scenario11() {
        postAndAssertResponse(organiseringResource, "");
        postAndAssertResponse(ressursResource, "");
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt feil signert token, får man Unauthenticated error i responsen")
    public void scenario12() {
        val rsaKey = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID("annenKey")
                .generate();
        val claims = securityTestUtils.validJwtClaims();
        val signer = new RSASSASigner(rsaKey);
        val signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("annenKey").build(), claims.build());
        signedJWT.sign(signer);

        postAndAssertResponse(organiseringResource, signedJWT.serialize());
        postAndAssertResponse(ressursResource, signedJWT.serialize());
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt ugyldig issuer i token, får man Unauthenticated error i responsen")
    public void scenario22() {
        val claims = securityTestUtils.validJwtClaims();
        claims.issuer("annenIssuer");
        val token = securityTestUtils.createToken(claims);

        postAndAssertResponse(organiseringResource, token);
        postAndAssertResponse(ressursResource, token);
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt ugyldig audience i token, får man Unauthenticated error i responsen")
    public void scenario25() {
        val claims = securityTestUtils.validJwtClaims();
        claims.audience("ugyldigAudience");
        val token = securityTestUtils.createToken(claims);

        postAndAssertResponse(organiseringResource, token);
        postAndAssertResponse(ressursResource, token);
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt utløpt token, får man Unauthenticated error i responsen")
    public void scenario26() {
        val claims = securityTestUtils.validJwtClaims();
        claims.expirationTime(Date.from(Instant.now().minusSeconds(60 * 60)));
        val token = securityTestUtils.createToken(claims);

        postAndAssertResponse(organiseringResource, token);
        postAndAssertResponse(ressursResource, token);
    }

    @Test
    @SneakyThrows
    @DisplayName("Gitt not before time frem i tid i tokenet, får man Unauthenticated error i responsen")
    public void scenario27() {
        val claims = securityTestUtils.validJwtClaims();
        claims.notBeforeTime(Date.from(Instant.now().plusSeconds(60 * 60)));
        val token = securityTestUtils.createToken(claims);

        postAndAssertResponse(organiseringResource, token);
        postAndAssertResponse(ressursResource, token);
    }

    @SneakyThrows
    private void postAndAssertResponse(String resource, String token) {
        val response = graphQLTestTemplate.withBearerAuth(token).postForResource(resource);
        assertThat(response, hasErrors());
        response.assertThatDataField().isNull();
        //response.assertThatListOfErrors().hasSize(1);
    }

}
