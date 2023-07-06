package no.nav.nom.apps.navident.common.security;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties("authentication")
public class AuthenticationProperties {
    private final Aad aad;

    @Value
    public static class Aad {
        String issuerBaseUrl;
        String clientId;
        String jwk;
    }
}
