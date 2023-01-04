package no.nav.nom.libs.utils.common.unleash;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.net.URI;

@ConstructorBinding
@ConfigurationProperties("unleash")
public record UnleashPropeties(
    Endpoint endpoint
) {
    public record Endpoint(URI url) {}
}
