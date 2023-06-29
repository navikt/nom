package no.nav.nom.apps.api.common.security;


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
