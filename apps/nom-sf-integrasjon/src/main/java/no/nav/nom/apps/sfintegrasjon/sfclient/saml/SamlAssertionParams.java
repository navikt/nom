package no.nav.nom.apps.sfintegrasjon.sfclient.saml;

import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.nom.apps.sfintegrasjon.sfclient.SfProperties;

/**
 * Override parameters for SAML assertion generation.
 * Null values fall back to values from {@link SfProperties}.
 */
public record SamlAssertionParams(
        @Schema(description = "OAuth client ID / SF API Key. Defaults to sf.client-id.") String clientId,
        @Schema(description = "PEM-encoded RSA private key. Defaults to sf.private-key.") String privateKey,
        @Schema(description = "SF API base URL. Defaults to sf.api-url.") String apiUrl,
        @Schema(description = "SF user ID for the assertion Subject. Defaults to sf.user-id.") String userId,
        @Schema(description = "SF company ID (tenant). Defaults to sf.tenant.") String tenant
) {
    public static SamlAssertionParams defaults() {
        return new SamlAssertionParams(null, null, null, null, null);
    }
}
