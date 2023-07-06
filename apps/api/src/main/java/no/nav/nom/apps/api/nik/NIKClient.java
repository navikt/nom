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
    private final WebClient webClient;
    private final WebClient onBehalfOfWebclient;

    public NIKClient(OnBehalfOfTokenRepository onBehalfOfTokenRepository, OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager, AppProperties appProperties) {

        ServletOAuth2AuthorizedClientExchangeFilterFunction oAuth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
        oAuth2Filter.setDefaultClientRegistrationId("nik");

        WebClient.Builder baseWebClientBuilder = WebClient.builder()
            .baseUrl(appProperties.clients().nik().baseUrl())
            .defaultHeaders(headers -> {
                headers.add("Nav-Consumer-Id", appProperties.name());
                headers.add("Nav-Call-Id", appProperties.name());
            });

        this.webClient = baseWebClientBuilder
            .filter(oAuth2Filter)
            .build();

        this.onBehalfOfWebclient = baseWebClientBuilder
            .filter((clientRequest, nextFilter) -> {
                clientRequest.headers().add(
                    "Authorization",
                    String.format("Bearer %s", onBehalfOfTokenRepository.getOnbehalfofTokenFor(appProperties.clients().nik().aadScope()))
                );
                return nextFilter.exchange(clientRequest);
            })
            .build();
    }

    public ResponseEntity<OpprettNavidentResponse> opprettNavident(OpprettNavidentRequest opprettNavidentRequest) {

        return opprettNavidentWithSuppliedWebClient(webClient, opprettNavidentRequest);
    }

    public ResponseEntity<OpprettNavidentResponse> opprettNavidentOnBehalfOfUser(OpprettNavidentRequest opprettNavidentRequest) {

        return opprettNavidentWithSuppliedWebClient(onBehalfOfWebclient, opprettNavidentRequest);
    }

    private ResponseEntity<OpprettNavidentResponse> opprettNavidentWithSuppliedWebClient(WebClient suppliedWebClient, OpprettNavidentRequest opprettNavidentRequest) {
        return suppliedWebClient.post()
            .uri("/navident")
            .bodyValue(opprettNavidentRequest)
            .retrieve()
            .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals, response -> Mono.empty()) // nav-ident has its own body type for errors actually caught.
            .toEntity(OpprettNavidentResponse.class)
            .block();
    }
}
