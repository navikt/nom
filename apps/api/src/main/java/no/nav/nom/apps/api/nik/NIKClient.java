package no.nav.nom.apps.api.nik;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.OnBehalfOfTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
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
            .apply(oAuth2Filter.oauth2Configuration())
            .build();

        this.nikWebClientBuilder = baseWebClientBuilder;

        this.nikScope = appProperties.clients().nik().aadScope();
        this.onBehalfOfTokenRepository = onBehalfOfTokenRepository;
    }

    public ResponseEntity<OpprettNavidentResponse> opprettNavident(OpprettNavidentRequest opprettNavidentRequest) {

        return opprettNavidentWithSuppliedWebClient(clientCredentialsAuthorizedWebClient, opprettNavidentRequest, "client_credentials");
    }

    public ResponseEntity<OpprettNavidentResponse> opprettNavidentOnBehalfOfUser(OpprettNavidentRequest opprettNavidentRequest) {
        String onBehalfOfToken = onBehalfOfTokenRepository.getOnbehalfofTokenFor(nikScope);

        var onBehalfOfAuthorizedWebClient = nikWebClientBuilder
            .defaultHeaders(httpHeaders -> httpHeaders.setBearerAuth(onBehalfOfToken))
            .build();

        return opprettNavidentWithSuppliedWebClient(onBehalfOfAuthorizedWebClient, opprettNavidentRequest, "on_behalf_of");
    }

    private ResponseEntity<OpprettNavidentResponse> opprettNavidentWithSuppliedWebClient(
        WebClient suppliedWebClient,
        OpprettNavidentRequest opprettNavidentRequest,
        String authMode
    ) {
        try {
            log.info("Oppretter navident gjennom NIK med authMode={}", authMode);
            return suppliedWebClient.post()
                .uri("/navident/create")
                .bodyValue(opprettNavidentRequest)
                .retrieve()
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals, response -> Mono.empty()) // nav-ident has its own body type for errors actually caught.
                .toEntity(OpprettNavidentResponse.class)
                .block();
        } catch (WebClientResponseException e) {
            if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
                log.warn(
                    "NIK /navident/create returned 401. authMode={}, www-authenticate='{}', responseBody='{}'",
                    authMode,
                    e.getHeaders().getFirst("www-authenticate"),
                    e.getResponseBodyAsString()
                );
            }
            throw e;
        }
    }

    public String lookupNavident(LookupNavidentRequest lookupNavidentRequest) {
        return lookupNavidentWithSuppliedWebClient(clientCredentialsAuthorizedWebClient, lookupNavidentRequest);
    }

    public String lookupNavidentOnBehalfOfUser(LookupNavidentRequest lookupNavidentRequest) {
        String onBehalfOfToken = onBehalfOfTokenRepository.getOnbehalfofTokenFor(nikScope);

        var onBehalfOfAuthorizedWebClient = nikWebClientBuilder
            .defaultHeaders(httpHeaders -> httpHeaders.setBearerAuth(onBehalfOfToken))
            .build();

        return lookupNavidentWithSuppliedWebClient(onBehalfOfAuthorizedWebClient, lookupNavidentRequest);
    }

    private String lookupNavidentWithSuppliedWebClient(WebClient suppliedWebClient, LookupNavidentRequest lookupNavidentRequest) {
        try {
            return suppliedWebClient.post()
                .uri("/navident/lookup")
                .bodyValue(lookupNavidentRequest)
                .retrieve()
                .onStatus(httpStatusCode -> !httpStatusCode.is2xxSuccessful(), ClientResponse::createException)
                .bodyToMono(LookupNavidentResponse.class)
                .map(LookupNavidentResponse::getNavident)
                .block();
        } catch (WebClientResponseException e) {
            log.info("Failed lookup of navident. Got status code " + e.getStatusCode());
        } catch (Exception ex) {
            log.info(NIKClient.class.getSimpleName() + ": Failed lookup of navident. Error type: " + ex.getClass());
        }
        return null;
    }
}
