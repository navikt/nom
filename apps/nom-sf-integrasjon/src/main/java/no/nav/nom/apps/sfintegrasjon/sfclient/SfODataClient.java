package no.nav.nom.apps.sfintegrasjon.sfclient;

import no.nav.nom.apps.sfintegrasjon.sfclient.oauth.SfAccessToken;
import no.nav.nom.apps.sfintegrasjon.sfclient.oauth.SfOAuthService;
import no.nav.nom.apps.sfintegrasjon.sfclient.saml.SamlAssertionParams;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for SAP SuccessFactors OData v2 API.
 * Obtains a fresh OAuth token before each request.
 *
 * <p>TODO: Add OData query methods as integration needs grow.
 * TODO: Consider token caching (reuse tokens until near expiry) to reduce assertion overhead.
 */
@Component
public class SfODataClient {

    private final SfProperties sfProperties;
    private final SfOAuthService sfOAuthService;
    private final RestClient.Builder restClientBuilder;

    public SfODataClient(
        SfProperties sfProperties,
        SfOAuthService sfOAuthService,
        RestClient.Builder restClientBuilder
    ) {
        this.sfProperties = sfProperties;
        this.sfOAuthService = sfOAuthService;
        this.restClientBuilder = restClientBuilder;
    }

    private RestClient authenticatedClient() {
        SfAccessToken token = sfOAuthService.generateToken(SamlAssertionParams.defaults());
        return restClientBuilder
            .baseUrl(sfProperties.apiUrl().replaceAll("/$", "") + "/odata/v2/")
            .defaultHeader("Authorization", "Bearer " + token.accessToken())
            .build();
    }
}
