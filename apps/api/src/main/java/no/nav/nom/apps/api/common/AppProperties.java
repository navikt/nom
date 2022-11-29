package no.nav.nom.apps.api.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties("app")
public record AppProperties(
        Proxy proxy,
        Pdl pdl,
        Nik nik,
        String name
) {

    public boolean isSandbox() {
        return name.equals("nom-sandbox-api");
    }

    public record Proxy(
            String baseUrl,
            String enhetPath,
            String aadClientId
    ) {}

    public record Pdl(
            String tema,
            String aadClientId,
            String baseUrl
    ) {}

    public record Nik(
            String baseUrl,
            String aadClientId
    ) {}
}
