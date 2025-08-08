package no.nav.nom.apps.api.clients.nomproxy;

import no.nav.nom.apps.api.common.AppProperties;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class NomProxyClient {

    private static final int MAX_IN_MEMORY_MB = 300;
    private final WebClient webClient;

    public NomProxyClient(OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager, AppProperties appProperties, WebClient.Builder builder) {

        ServletOAuth2AuthorizedClientExchangeFilterFunction oAuth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
        oAuth2Filter.setDefaultClientRegistrationId("nom-proxy");
        this.webClient = builder.clone()
            .exchangeStrategies(
                ExchangeStrategies.builder().codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_MB * 1024 * 1024)).build()
            )
            .filter(oAuth2Filter)
            .baseUrl(appProperties.clients().nomProxy().baseUrl())
            .build();
    }

    public WebClient getWebClient() {
        return webClient;
    }
}
