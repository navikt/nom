package no.nav.nom.apps.sfintegrasjon.nikclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class NIKClient {

    private static final String NIK_CLIENT_REGISTRATION_ID = "nik";

    private final RestClient nikRestClient;

    public NIKClient(
        NikProperties nikProperties,
        OAuth2AuthorizedClientManager authorizedClientManager
    ) {
        var oauth2Interceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        oauth2Interceptor.setClientRegistrationIdResolver(_ -> NIK_CLIENT_REGISTRATION_ID);

        this.nikRestClient = RestClient.builder()
            .baseUrl(nikProperties.baseUrl())
            .defaultHeaders(headers -> {
                headers.add("Nav-Consumer-Id", "Zalaris");
                headers.add("Nav-Call-Id", "nom-sf-integrasjon");
            })
            .requestInterceptor(oauth2Interceptor)
            .build();
    }

    public ResponseEntity<OpprettNavidentResponse> opprettNavident(OpprettNavidentRequest opprettNavidentRequest) {
        return nikRestClient.post()
            .uri("/navident/create")
            .body(opprettNavidentRequest)
            .retrieve()
            .toEntity(OpprettNavidentResponse.class);
    }
}
