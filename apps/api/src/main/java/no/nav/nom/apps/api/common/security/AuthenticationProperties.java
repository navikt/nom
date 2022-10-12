package no.nav.nom.apps.api.common.security;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties("authentication")
public record AuthenticationProperties (
    Aad aad
){

    public record Aad (
        String issuerBaseUrl,
        String clientId,
        String jwk
    ){}
}
