package no.nav.nom.apps.api.graphql;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static java.lang.String.format;

@Component
@RequiredArgsConstructor
public class SecurityTestUtils {
    private final RSAKey rsaKey;
    private final Environment env;

    public JWTClaimsSet.Builder validJwtClaims() {
        val wiremockPort = env.getRequiredProperty("wiremock.server.port");

        return new JWTClaimsSet.Builder()
                .subject("testSubject")
                .audience("test")
                .jwtID(UUID.randomUUID().toString())
                .issueTime(new Date())
                .issuer(format("http://localhost:%s/aad", wiremockPort))
                .expirationTime(Date.from(Instant.now().plusSeconds(60 * 60)));
    }

    @SneakyThrows
    public String createToken(JWTClaimsSet.Builder builder) {
        val signer = new RSASSASigner(rsaKey);
        val signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(), builder.build());
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}
