package no.nav.nom.apps.sfintegrasjon.sfclient.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth 2.0 access token response from SAP SuccessFactors.
 */
public record SfAccessToken(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") int expiresIn
) {}
