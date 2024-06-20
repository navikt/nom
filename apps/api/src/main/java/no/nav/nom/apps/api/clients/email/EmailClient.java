package no.nav.nom.apps.api.clients.email;

import no.nav.nom.apps.api.common.AppProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class EmailClient {

    public static final Logger logger = LogManager.getLogger(EmailClient.class);

    private final WebClient webClient;
    private final AppProperties.Email emailClientProperties;

    public EmailClient(
        OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
        AppProperties appProperties
    ) {
        this.emailClientProperties = appProperties.clients().email();
        ServletOAuth2AuthorizedClientExchangeFilterFunction oAuth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
        oAuth2Filter.setDefaultClientRegistrationId("email");

        this.webClient = WebClient.builder()
            .filter(oAuth2Filter)
            .baseUrl(emailClientProperties.baseUrl())
            .build();
    }

    public Mono<Void> sendEmail(EmailMessage emailMessage) {
        if (emailClientProperties.isEnabled()) {
            logger.info("Attempting sending email to: {}, url: {}",
                emailMessage.message().toRecipients(),
                emailClientProperties.baseUrl());
            return webClient.post()
                .body(Mono.just(emailMessage), EmailMessage.class)
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(defaultRetry(emailClientProperties.maxAttempts(), emailClientProperties.backoffDurationMillis()))
                .doOnError(WebClientResponseException.class, (WebClientResponseException webClientResponseException) -> {
                    String errorMsg = String.format(
                        "POST %s resulted in status %s with body %s",
                        webClientResponseException.getRequest() != null ? webClientResponseException.getRequest().getURI() : "n/a",
                        webClientResponseException.getStatusCode().value(),
                        webClientResponseException.getResponseBodyAsString()
                    );
                    logger.warn("Sending email fails: " + errorMsg, webClientResponseException);
                    throw new EmailClientUnforseenException(errorMsg);
                });
        } else {
            logger.info("Email which would have been sent: {}", emailMessage);
            return Mono.empty();
        }
    }

    private Retry defaultRetry(Integer maxAttempts, Integer backoffDurationMillis) {
        return Retry.backoff(maxAttempts, Duration.ofMillis(backoffDurationMillis)).jitter(0.75).filter(throwable -> {
            if (throwable instanceof WebClientResponseException) {
                return ((WebClientResponseException) throwable).getStatusCode().is5xxServerError();
            }
            return false;
        }).onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
            Throwable throwable = retrySignal.failure();
            if (throwable instanceof WebClientResponseException) {
                throw (WebClientResponseException) throwable;
            }
            throw new RuntimeException(throwable);
        });
    }

}
