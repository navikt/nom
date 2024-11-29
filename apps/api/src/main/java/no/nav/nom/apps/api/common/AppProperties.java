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
        NomRemedyIntegrasjon nomRemedyIntegrasjon,
        DvhOrds dvhOrds
    ) {}

    public record NomProxy(
            String baseUrl,
            String enhetPath,
            String ressursStillingerPath,
            String eksterneRessurserPath,
            String kommunaleRessurserPath,
            String ansatteBucketsPath,
            String tlfPath,
            String orgHierarkiPath
        ) {}

    public record Pdl(
            String tema,
            String behandlingsnummer,
            String baseUrl,
            String aadScope
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

    public record DvhOrds(
        String clientId,
        String clientSecret,
        String tokenUri,
        String baseUrl,
        String eksterneRessurserPath
    ) {}
}
