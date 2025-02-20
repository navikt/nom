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

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static no.nav.nom.apps.api.clients.WebClientUtils.defaultRetry;

@Service
public class KontaktinfoClient {

    public static final Logger log = LogManager.getLogger(KontaktinfoClient.class);
    private final WebClient webClient;
    private final OnBehalfOfTokenRepository onBehalfOfTokenRepository;
    private final String paarorendeScope;

    @Value("${app.clients.paarorende.max-attempts}")
    private Integer maxAttempts;

    @Value("${app.clients.paarorende.backoff-duration-millis}")
    private Integer backoffDurationMillis;

    public KontaktinfoClient(
            @Value("${app.clients.paarorende.base-url}") String baseUrl,
            OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
            OnBehalfOfTokenRepository onBehalfOfTokenRepository,
            AppProperties appProperties
    ) throws IOException {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oAuth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
        oAuth2Filter.setDefaultClientRegistrationId("paarorende");

        this.onBehalfOfTokenRepository = onBehalfOfTokenRepository;
        this.paarorendeScope = appProperties.clients().paarorende().aadScope();
        this.webClient = WebClient.builder()
            .filter(oAuth2Filter)
            .baseUrl(baseUrl)
            .build();
    }

    public List<Kontaktinfo> getKontaktinfo(String navident) {
        String onBehalfOfToken = onBehalfOfTokenRepository.getOnbehalfofTokenFor(paarorendeScope);
        return webClient.get()
            .uri(String.format("/api/kontaktinfo/%s", navident))
            .headers(httpHeaders -> httpHeaders.setBearerAuth(onBehalfOfToken))
            .retrieve()
            .toEntityList(Kontaktinfo.class)
            .retryWhen(defaultRetry(maxAttempts, backoffDurationMillis))
            .doOnError(WebClientResponseException.class, (webClientResponseException) -> {
                if (webClientResponseException.getStatusCode().value() == 404) {
                    log.error("Kontaktinfo not found", webClientResponseException);
                    throw new KontaktinfoException("Kontaktinfo not found", ErrorCode.NOT_FOUND);
                }

                String errorMsg = String.format(
                    "GET kontaktinfo %s resulted in status %s with body %s",
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
