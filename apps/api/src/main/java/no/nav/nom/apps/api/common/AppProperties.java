package no.nav.nom.apps.api.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
            String orgtilknytningPath,
            String ressursStillingerPath,
            String aadClientId
    ) {}

    public record Pdl(
            String tema,
            String behandlingsnummer,
            String aadClientId,
            String baseUrl
    ) {}

    public record Nik(
            String baseUrl,
            String aadClientId
    ) {}
}
