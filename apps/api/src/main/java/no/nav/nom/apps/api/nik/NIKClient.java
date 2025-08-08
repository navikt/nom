package no.nav.nom.apps.api.nik;

import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.OnBehalfOfTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class NIKClient {
    private final WebClient clientCredentialsAuthorizedWebClient;
    private final WebClient.Builder nikWebClientBuilder;

    private final OnBehalfOfTokenRepository onBehalfOfTokenRepository;
    private final String nikScope;

    public NIKClient(OnBehalfOfTokenRepository onBehalfOfTokenRepository, OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
                     WebClient.Builder builder, AppProperties appProperties) {

        ServletOAuth2AuthorizedClientExchangeFilterFunction oAuth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
        oAuth2Filter.setDefaultClientRegistrationId("nik");

        WebClient.Builder baseWebClientBuilder = builder.clone()
            .baseUrl(appProperties.clients().nik().baseUrl())
            .defaultHeaders(headers -> {
                headers.add("Nav-Consumer-Id", appProperties.name());
                headers.add("Nav-Call-Id", appProperties.name());
            });

        this.clientCredentialsAuthorizedWebClient = baseWebClientBuilder
            .filter(oAuth2Filter)
            .build();

        this.nikWebClientBuilder = baseWebClientBuilder;

        this.nikScope = appProperties.clients().nik().aadScope();
        this.onBehalfOfTokenRepository = onBehalfOfTokenRepository;
    }

    public ResponseEntity<OpprettNavidentResponse> opprettNavident(OpprettNavidentRequest opprettNavidentRequest) {

        return opprettNavidentWithSuppliedWebClient(clientCredentialsAuthorizedWebClient, opprettNavidentRequest);
    }

    public ResponseEntity<OpprettNavidentResponse> opprettNavidentOnBehalfOfUser(OpprettNavidentRequest opprettNavidentRequest) {
        String onBehalfOfToken = onBehalfOfTokenRepository.getOnbehalfofTokenFor(nikScope);

        var onBehalfOfAuthorizedWebClient = nikWebClientBuilder
            .defaultHeaders(httpHeaders -> httpHeaders.setBearerAuth(onBehalfOfToken))
            .build();

        return opprettNavidentWithSuppliedWebClient(onBehalfOfAuthorizedWebClient, opprettNavidentRequest);
    }

    private ResponseEntity<OpprettNavidentResponse> opprettNavidentWithSuppliedWebClient(WebClient suppliedWebClient, OpprettNavidentRequest opprettNavidentRequest) {

        return suppliedWebClient.post()
            .uri("/navident/create")
            .bodyValue(opprettNavidentRequest)
            .retrieve()
            .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals, response -> Mono.empty()) // nav-ident has its own body type for errors actually caught.
            .toEntity(OpprettNavidentResponse.class)
            .block();
    }
}
