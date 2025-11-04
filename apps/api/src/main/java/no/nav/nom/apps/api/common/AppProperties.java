package no.nav.nom.apps.api.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(
        Clients clients,
        String name,
        EpostVarselForEnhetNavnendring epostVarselForEnhetNavnendring,
        DollyRessurs dollyRessurs
) {

    public boolean isSandbox() {
        return name.equals("nom-sandbox-api");
    }

    public record Clients(
        NomProxy nomProxy,
        Nik nik,
        Pdl pdl,
        MicrosoftGraph microsoftGraph,
        NomRemedyIntegrasjon nomRemedyIntegrasjon,
        DvhOrds dvhOrds,
        NomPaarorende nomPaarorende
    ) {}

    public record NomProxy(
            String baseUrl
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

    public record MicrosoftGraph(
        String baseUrl,
        String aadClientId,
        Integer maxAttempts,
        Integer backoffDurationMillis,
        Boolean sendEmail
    ) {}

    public record NomRemedyIntegrasjon(
        String baseUrl
    ) {}

    public record DvhOrds(
        String clientId,
        String clientSecret,
        String tokenUri,
        String baseUrl,
        String enhetPath,
        String ressursStillingerPath,
        String eksterneRessurserPath,
        String ansatteBucketsPath,
        String tlfPath,
        String orgHierarkiPath,
        String nomRessurserPath
    ) {}

    public record NomPaarorende(
        String baseUrl,
        String aadScope
    ) {}

    public record EpostVarselForEnhetNavnendring(
        Integer maxEvtAlderDagerForVarsel
    ) {}

    public record DollyRessurs(
        String orgenhetId,
        String fysiskBrukerstedNr
    ) {}
}
