package no.nav.nom.apps.api.dvh;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Getter
@ConstructorBinding
@AllArgsConstructor
@ConfigurationProperties("aad")
public class AadProperties {
    private final String issuerBaseUrl;
    private final String clientId;
    private final String clientAssertionType;
    private final String jwk;
    private final String grantType;

    private final String dvhScope;
}
