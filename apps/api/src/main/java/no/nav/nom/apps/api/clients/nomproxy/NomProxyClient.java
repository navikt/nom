package no.nav.nom.apps.api.clients.nomproxy;

import no.nav.nom.apps.api.common.AppProperties;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class NomProxyClient {

    private final WebClient webClient;

    public NomProxyClient(OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager, AppProperties appProperties) {

        ServletOAuth2AuthorizedClientExchangeFilterFunction oAuth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
        oAuth2Filter.setDefaultClientRegistrationId("nom-proxy");
        this.webClient = WebClient.builder()
            .exchangeStrategies(
                ExchangeStrategies.builder().codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(30000000)).build()
            )
            .filter(oAuth2Filter)
            .baseUrl(appProperties.clients().nomProxy().baseUrl())
            .build();
    }

    public WebClient getWebClient() {
        return webClient;
    }
}
