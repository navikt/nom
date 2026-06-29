package no.nav.nom.apps.navident.pdl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import static no.nav.nom.libs.utils.common.mdc.MdcConfig.HEADER_CONSUMER_ID_KEY;

@Configuration
public class PdlWebClientConfig {

    @Bean
    public OAuth2AuthorizedClientManager pdlAuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .clientCredentials()
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    @Bean
    public RestClient pdlRestClient(
            PdlConfig pdlConfig,
            @Qualifier("pdlAuthorizedClientManager") OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
            RestClient.Builder builder
    ) {
        OAuth2ClientHttpRequestInterceptor oauth2Interceptor = new OAuth2ClientHttpRequestInterceptor(oAuth2AuthorizedClientManager);
        oauth2Interceptor.setClientRegistrationIdResolver(request -> "pdl");

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(pdlConfig.getTimeoutInMillis());
        requestFactory.setConnectTimeout(pdlConfig.getTimeoutInMillis());

        return builder
                .requestFactory(requestFactory)
                .requestInterceptor(oauth2Interceptor)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HEADER_CONSUMER_ID_KEY, pdlConfig.getAppName())
                .defaultHeader("Tema", pdlConfig.getTema())
                .defaultHeader("behandlingsnummer", pdlConfig.getBehandlingsnummer())
                .build();
    }
}
