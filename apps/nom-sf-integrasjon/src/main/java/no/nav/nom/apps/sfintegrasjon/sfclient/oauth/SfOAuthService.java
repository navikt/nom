package no.nav.nom.apps.sfintegrasjon.sfclient.oauth;

import no.nav.nom.apps.sfintegrasjon.sfclient.SfProperties;
import no.nav.nom.apps.sfintegrasjon.sfclient.saml.SamlAssertionParams;
import no.nav.nom.apps.sfintegrasjon.sfclient.saml.SamlAssertionService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Exchanges a SAML assertion for a SAP SuccessFactors OAuth 2.0 access token
 * using the SAML 2.0 Bearer Grant (RFC 7522).
 */
@Service
public class SfOAuthService {

    private static final String SAML_BEARER_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:saml2-bearer";

    private final SfProperties sfProperties;
    private final SamlAssertionService samlAssertionService;
    private final RestClient restClient;

    public SfOAuthService(
        SfProperties sfProperties,
        SamlAssertionService samlAssertionService,
        RestClient.Builder restClientBuilder
    ) {
        this.sfProperties = sfProperties;
        this.samlAssertionService = samlAssertionService;
        this.restClient = restClientBuilder
            .baseUrl(sfProperties.apiUrl())
            .build();
    }

    /**
     * Validates an existing SAP SuccessFactors OAuth access token.
     *
     * @param token Raw Bearer token value (without the "Bearer " prefix)
     * @return Raw response body from SAP SuccessFactors
     */
    public String validateToken(String token) {
        return restClient.get()
            .uri("/oauth/validate")
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .toEntity(String.class)
            .getBody();
    }

    /**
     * Generates a SAML assertion and exchanges it for an SF OAuth access token.
     *
     * @param params Override parameters for assertion generation; null fields fall back to {@link SfProperties}
     * @return Access token from SAP SuccessFactors
     */
    public SfAccessToken generateToken(SamlAssertionParams params) {
        String assertion = samlAssertionService.generateAssertion(params);
        String clientId = params.clientId() != null ? params.clientId() : sfProperties.clientId();
        String tenant = params.tenant() != null ? params.tenant() : sfProperties.tenant();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", SAML_BEARER_GRANT_TYPE);
        form.add("client_id", clientId);
        form.add("company_id", tenant);
        form.add("assertion", assertion);

        return restClient.post()
            .uri("/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(SfAccessToken.class);
    }
}
