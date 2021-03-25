package no.nav.nom.apps.api.common.security;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Getter
@ConstructorBinding
@AllArgsConstructor
@ConfigurationProperties("authentication")
public class AuthenticationProperties {
    private final Aad aad;
    private final Sts sts;

    @Value
    public static class Aad {
        String issuerBaseUrl;
        String clientId;
        String clientAssertionType;
        String jwk;
        String grantType;
    }

    @Value
    public static class Sts {
        String issuerBaseUrl;
        String username;
        String password;
        String grantType;
        String scope;
    }
}
