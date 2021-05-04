package no.nav.nom.apps.proxy.common;

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
    private final Sts sts;

    @Value
    public static class Sts {
        String issuerBaseUrl;
        String username;
        String password;
        String grantType;
        String scope;
    }
}