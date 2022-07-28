package no.nav.nom.libs.test;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static java.lang.String.format;
import static no.nav.nom.libs.test.RsaStub.RSA_KEY;

@Component
@RequiredArgsConstructor
public class SecurityTestUtils {

    private final Environment env;

    public JWTClaimsSet.Builder validJwtClaims() {
        return validJwtClaims("testSubject", "test");
    }

    public JWTClaimsSet.Builder validJwtClaims(String subject, String audience) {
        val wiremockPort = env.getRequiredProperty("wiremock.server.port");

        return new JWTClaimsSet.Builder()
                .subject(subject)
                .audience(audience)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(new Date())
                .issuer(format("http://localhost:%s/aad", wiremockPort))
                .expirationTime(Date.from(Instant.now().plusSeconds(60 * 60)));
    }

    @SneakyThrows
    public SignedJWT createSignedJWT(JWTClaimsSet.Builder builder) {
        val signer = new RSASSASigner(RSA_KEY);
        val signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(RSA_KEY.getKeyID()).build(), builder.build());
        signedJWT.sign(signer);
        return signedJWT;
    }

    @SneakyThrows
    public String createToken(JWTClaimsSet.Builder builder) {
        return createSignedJWT(builder).serialize();
    }

    @Bean
    public RSAKey rsaKey() {
        return RSA_KEY;
    }
}
