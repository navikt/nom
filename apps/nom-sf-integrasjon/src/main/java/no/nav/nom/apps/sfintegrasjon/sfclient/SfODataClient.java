package no.nav.nom.apps.sfintegrasjon.sfclient;

import no.nav.nom.apps.sfintegrasjon.sfclient.oauth.SfAccessToken;
import no.nav.nom.apps.sfintegrasjon.sfclient.oauth.SfTokenCache;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for SAP SuccessFactors OData v2 API.
 * Reuses a cached OAuth token; the cache proactively refreshes when fewer than 10 minutes remain before expiry.
 *
 * <p>TODO: Add OData query methods as integration needs grow.
 */
@Component
public class SfODataClient {

    private final SfProperties sfProperties;
    private final SfTokenCache sfTokenCache;
    private final RestClient.Builder restClientBuilder;

    public SfODataClient(
        SfProperties sfProperties,
        SfTokenCache sfTokenCache,
        RestClient.Builder restClientBuilder
    ) {
        this.sfProperties = sfProperties;
        this.sfTokenCache = sfTokenCache;
        this.restClientBuilder = restClientBuilder;
    }

    /**
     * Fetches the complete OData $metadata document for all SF entities.
     *
     * @return Raw XML metadata response from SAP SuccessFactors
     */
    public String getAllMetadata() {
        return authenticatedClient()
            .get()
            .uri("$metadata")
            .retrieve()
            .body(String.class);
    }

    /**
     * Fetches the OData $metadata document for the given SF entity.
     *
     * @param entity Entity name (letters only, validated by caller)
     * @return Raw XML metadata response from SAP SuccessFactors
     */
    public String getEntityMetadata(String entity) {
        return authenticatedClient()
            .get()
            .uri(entity + "/$metadata")
            .retrieve()
            .body(String.class);
    }

    private RestClient authenticatedClient() {
        SfAccessToken token = sfTokenCache.getToken();
        return restClientBuilder
            .baseUrl(sfProperties.apiUrl().replaceAll("/$", "") + "/odata/v2/")
            .defaultHeader("Authorization", "Bearer " + token.accessToken())
            .build();
    }
}
