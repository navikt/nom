package no.nav.nom.apps.sfintegrasjon.sfclient.saml;

import no.nav.nom.apps.sfintegrasjon.sfclient.SfProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SfCpiClient {

    private static final String CPI_CLIENT_REGISTRATION_ID = "sap-cpi";

    private final RestClient cpiClient;

    public SfCpiClient(
        SfProperties sfProperties,
        OAuth2AuthorizedClientManager authorizedClientManager
    ) {
        var oauth2Interceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        oauth2Interceptor.setClientRegistrationIdResolver(_ -> CPI_CLIENT_REGISTRATION_ID);

        this.cpiClient = RestClient.builder()
            .baseUrl(sfProperties.cpiApiUrl())
            .requestInterceptor(oauth2Interceptor)
            .build();
    }

    public ResponseEntity<String> sendOrgenhetPayload(String payload) {
        return cpiClient.post()
            .uri("http/NOM_to_Zalaris_Org_Data")
            .body(payload)
            .retrieve()
            .toEntity(String.class);
    }
}
