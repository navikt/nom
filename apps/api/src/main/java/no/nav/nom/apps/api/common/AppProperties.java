package no.nav.nom.apps.api.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(
        Clients clients,
        String name
) {

    public boolean isSandbox() {
        return name.equals("nom-sandbox-api");
    }

    public record Clients(
        NomProxy nomProxy,
        Nik nik,
        Pdl pdl,
        Email email,
        NomRemedyIntegrasjon nomRemedyIntegrasjon
    ) {}

    public record NomProxy(
            String baseUrl,
            String enhetPath,
            String orgtilknytningPath,
            String ressursStillingerPath,
            String eksterneRessurserPath
    ) {}

    public record Pdl(
            String tema,
            String behandlingsnummer,
            String baseUrl
    ) {}

    public record Nik(
            String baseUrl,
            String aadScope
    ) {}

    public record Email(
        String baseUrl,
        String aadClientId,
        Integer maxAttempts,
        Integer backoffDurationMillis,
        Boolean isEnabled
    ) {}

    public record NomRemedyIntegrasjon(
        String baseUrl
    ) {}
}
