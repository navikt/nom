package no.nav.nom.apps.sfintegrasjon.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.sfintegrasjon.orgenhet.NomProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
public class RestClientConfig {

    private final NomProperties nomProperties;

    public RestClientConfig(NomProperties nomProperties) {
        this.nomProperties = nomProperties;
    }

    @Bean
    public RestClient nomApiAuthorizedRestClient(OAuth2AuthorizedClientManager authorizedClientManager, RestClient.Builder builder) {
        OAuth2ClientHttpRequestInterceptor oauth2Interceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        oauth2Interceptor.setClientRegistrationIdResolver(_ -> "nom-api");

        return builder
            .baseUrl(nomProperties.baseUrl())
            .requestInterceptor(oauth2Interceptor)
            .build();
    }
}
