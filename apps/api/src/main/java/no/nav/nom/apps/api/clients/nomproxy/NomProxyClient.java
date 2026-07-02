package no.nav.nom.apps.api.clients.nomproxy;

import no.nav.nom.apps.api.common.AppProperties;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NomProxyClient {

    private final RestClient restClient;

    public NomProxyClient(OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager, AppProperties appProperties, RestClient.Builder builder) {
        OAuth2ClientHttpRequestInterceptor oauth2Interceptor = new OAuth2ClientHttpRequestInterceptor(oAuth2AuthorizedClientManager);
        oauth2Interceptor.setClientRegistrationIdResolver(request -> "nom-proxy");

        this.restClient = builder
            .baseUrl(appProperties.clients().nomProxy().baseUrl())
            .requestInterceptor(oauth2Interceptor)
            .build();
    }

    public RestClient getRestClient() {
        return restClient;
    }
}
