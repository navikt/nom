package no.nav.nom.apps.api.clients.kontaktinfo;

import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.OnBehalfOfTokenRepository;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static no.nav.nom.apps.api.clients.WebClientUtils.defaultRetry;

@Service
public class KontaktinfoClient {

    public static final Logger log = LogManager.getLogger(KontaktinfoClient.class);
    private final String paarorendeScope;
    private final OnBehalfOfTokenRepository onBehalfOfTokenRepository;
    private final String kontaktinfoBaseUrl;

    @Value("${app.clients.nom-paarorende.max-attempts}")
    private Integer maxAttempts;

    @Value("${app.clients.nom-paarorende.backoff-duration-millis}")
    private Integer backoffDurationMillis;

    public KontaktinfoClient(
            OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
            OnBehalfOfTokenRepository onBehalfOfTokenRepository,
            AppProperties appProperties
    ) throws IOException {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oAuth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
        oAuth2Filter.setDefaultClientRegistrationId("nom-paarorende");
        this.onBehalfOfTokenRepository = onBehalfOfTokenRepository;
        this.paarorendeScope = appProperties.clients().nomPaarorende().aadScope();
        kontaktinfoBaseUrl = appProperties.clients().nomPaarorende().baseUrl();
    }

    public List<RessursKontaktinfo> getKontaktinfoForRessurser(List<String> navidents, String orgenhetId) {
        String onBehalfOfToken = onBehalfOfTokenRepository.getOnbehalfofTokenFor(paarorendeScope);

        var onBehalfOfAuthorizedWebClient = WebClient.builder()
            .baseUrl(kontaktinfoBaseUrl)
            .defaultHeaders(httpHeaders -> httpHeaders.setBearerAuth(onBehalfOfToken))
            .build();

        return onBehalfOfAuthorizedWebClient.post()
                //.uri("/api/kontaktinfo/ressurser")
                .uri(uriBuilder -> uriBuilder
                    .path("/api/kontaktinfo/ressurser/" + "{orgenhetId}")
                    .build(orgenhetId))
                .body(Mono.just(navidents), List.class)
                .retrieve()
                .toEntityList(RessursKontaktinfo.class)
                .retryWhen(defaultRetry(maxAttempts, backoffDurationMillis))
                .doOnError(WebClientResponseException.class, (webClientResponseException) -> {
                    if (webClientResponseException.getStatusCode().value() == 404) {
                        log.error("RessursKontaktinfo not found", webClientResponseException);
                        throw new KontaktinfoException("RessursKontaktinfo not found", ErrorCode.NOT_FOUND);
                    }

                    String errorMsg = String.format(
                            "GET RessursKontaktinfo %s resulted in status %s with body %s",
                            Objects.requireNonNull(webClientResponseException.getRequest()).getURI(),
                            webClientResponseException.getStatusCode().value(),
                            webClientResponseException.getResponseBodyAsString()
                    );
                    log.error(errorMsg, webClientResponseException);
                    throw new KontaktinfoException(errorMsg, ErrorCode.SERVER_ERROR);
                })
                .block().getBody();
    }

}
