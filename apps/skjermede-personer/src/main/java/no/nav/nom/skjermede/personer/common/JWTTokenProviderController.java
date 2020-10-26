package no.nav.nom.skjermede.personer.common;

import lombok.val;
import no.nav.security.token.support.core.api.Unprotected;
import no.nav.security.token.support.test.JwkGenerator;
import no.nav.security.token.support.test.JwtTokenGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Profile({"test", "local"})
@RequestMapping({"/local"})
public class JWTTokenProviderController {

    private final Map<String, String> issuerMap = Map.of(
            "selvbetjening", "iss-localhost",
            "reststs-testonly", "iss-localhost-2",
            "reststs", "iss-localhost-3"
    );

    @Unprotected
    @GetMapping({"/jwt"})
    public String issueToken(@RequestParam(value = "subject", defaultValue = "12345678910") final String subject,
                             @RequestParam(value = "issuer", defaultValue = "selvbetjenning") final String issuer) {
        if (!issuerMap.containsKey(issuer)) {
            return "Unknown issuer";
        }

        val expiry = TimeUnit.MINUTES.toMillis(12960000L);
        val claimsSet = JwtTokenGenerator.buildClaimSet(subject, issuerMap.get(issuer), "aud-localhost", "Level4", expiry);

        return JwtTokenGenerator.createSignedJWT(JwkGenerator.getDefaultRSAKey(), claimsSet).serialize();
    }
}
