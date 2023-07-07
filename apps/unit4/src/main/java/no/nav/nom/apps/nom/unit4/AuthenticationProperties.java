package no.nav.nom.apps.nom.unit4;

import com.nimbusds.jose.jwk.JWK;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("authentication")
public record AuthenticationProperties(
        Aad aad

) {
    public record Aad(
            String issuerBaseUrl,
            String clientId,
            JWK jwk
    ) {}
}
