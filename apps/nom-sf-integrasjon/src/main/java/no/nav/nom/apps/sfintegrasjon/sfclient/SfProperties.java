package no.nav.nom.apps.sfintegrasjon.sfclient;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for SAP SuccessFactors integration.
 * All values are injected from the Nais secret {@code sap-sf-credentials} via environment variables.
 */
@ConfigurationProperties("app.sf")
public record SfProperties(
        String apiUrl,
        String clientId,
        String privateKey,
        String tenant,
        String userId
) {}
