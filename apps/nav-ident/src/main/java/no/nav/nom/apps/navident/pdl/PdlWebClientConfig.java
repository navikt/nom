package no.nav.nom.apps.navident.pdl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

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
    public WebClient pdlWebClient(
            PdlConfig pdlConfig, 
            @Qualifier("pdlAuthorizedClientManager") OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager
    ) {

        ServletOAuth2AuthorizedClientExchangeFilterFunction oAuth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
        oAuth2Filter.setDefaultClientRegistrationId("pdl");

        return WebClient.builder()
                .clone()
                .filter(oAuth2Filter)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HEADER_CONSUMER_ID_KEY, pdlConfig.getAppName())
                .defaultHeader("Tema", pdlConfig.getTema())
                .defaultHeader("behandlingsnummer", pdlConfig.getBehandlingsnummer())
                .build();
    }
}
