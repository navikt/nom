package no.nav.nom.libs.utils.common.unleash;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties("unleash")
public record UnleashProperties(
    Endpoint endpoint,
    String apiKey
) {
    public record Endpoint(URI url) {}
}
