package no.nav.nom.apps.api.clients.msgraph;

import no.nav.nom.apps.api.common.AppProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class MicrosoftGraphClient {

    public static final Logger logger = LogManager.getLogger(MicrosoftGraphClient.class);

    private static final String EMAIL_URI_PATH = "/users/ikke.svar.nom@nav.no/sendMail";

    private final WebClient webClient;
    private final AppProperties.MicrosoftGraph msGraphClientProperties;

    public MicrosoftGraphClient(
            @Qualifier("authorizedClientManager") OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
            AppProperties appProperties
    ) {
        this.msGraphClientProperties = appProperties.clients().microsoftGraph();
        ServletOAuth2AuthorizedClientExchangeFilterFunction oAuth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
        oAuth2Filter.setDefaultClientRegistrationId("email");

        this.webClient = WebClient.builder()
                .filter(oAuth2Filter)
                .baseUrl(msGraphClientProperties.baseUrl())
                .build();
    }

    public Mono<Void> sendEmail(EmailMessage emailMessage) {
        if (msGraphClientProperties.sendEmail()) {
            logger.info("Attempting to send email: {}, url: {}",
                    emailMessage.message().toRecipients(),
                    msGraphClientProperties.baseUrl() + EMAIL_URI_PATH);
            return webClient.post()
                    .uri(EMAIL_URI_PATH)
                    .body(Mono.just(emailMessage), EmailMessage.class)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .retryWhen(defaultRetry(msGraphClientProperties.maxAttempts(), msGraphClientProperties.backoffDurationMillis()))
                    .doOnError(Exception.class, (Exception exception) -> {
                        String errorMsg = exception.getMessage();
                        if (exception instanceof WebClientResponseException) {
                            errorMsg = String.format(
                                    "POST %s resulted in status %s with body %s",
                                    ((WebClientResponseException) exception).getRequest() != null ?
                                            ((WebClientResponseException) exception).getRequest().getURI() : "n/a",
                                    ((WebClientResponseException) exception).getStatusCode().value(),
                                    ((WebClientResponseException) exception).getResponseBodyAsString()
                            );
                        }
                        logger.warn("Sending email failed: {}", errorMsg);
                        throw new EmailSendingUnforeseenException(errorMsg);
                    });
        } else {
            logger.info("Email which would have been sent: {}", emailMessage);
            return Mono.empty();
        }
    }

    public List<EntraIdUser> getUsersForGroup(String groupId) {
        logger.debug("Getting users for group: {}", groupId);

        String uri = "/groups/%s/members?$select=mail,onPremisesSamAccountName".formatted(groupId);
        ODataResponse<EntraIdUser> response = getUsersFromUri(uri);

        List<EntraIdUser> users = new ArrayList<>(response.value());
        String nextLink = response.nextLink();

        while (nextLink != null) {
            response = getUsersFromUri(nextLink);

            if (response != null) {
                users.addAll(response.value());
                nextLink = response.nextLink();
            } else {
                nextLink = null;
            }
        }

        logger.info("Found {} users in group {}", users.size(), groupId);
        return users;
    }

    private ODataResponse<EntraIdUser> getUsersFromUri(String uri) {
        logger.debug("Getting users from URI: {}", uri);
        return webClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse ->
                        clientResponse.bodyToMono(ODataError.class)
                                .flatMap(oDataError -> {
                                    logger.warn("GET failed for URI '{}': Response code: '{}' Error code: '{}' Error message: '{}'",
                                            uri, clientResponse.statusCode(), oDataError.error().code(), oDataError.error().message());
                                    return Mono.empty();
                                })
                )
                .bodyToMono(new ParameterizedTypeReference<ODataResponse<EntraIdUser>>() {
                })
                .defaultIfEmpty(emptyODataResponse())
                .retryWhen(defaultRetry(msGraphClientProperties.maxAttempts(), msGraphClientProperties.backoffDurationMillis()))
                .doOnError(Exception.class, exception ->
                        logger.warn("GET failed for URI '{}': {}", uri, exception.getMessage(), exception))
                .onErrorReturn(emptyODataResponse())
                .block();
    }

    private <T> ODataResponse<T> emptyODataResponse() {
        return ODataResponse.<T>builder().value(Collections.emptyList()).build();
    }

    private Retry defaultRetry(Integer maxAttempts, Integer backoffDurationMillis) {
        return Retry.backoff(maxAttempts, Duration.ofMillis(backoffDurationMillis))
                .jitter(0.75)
                .filter(throwable -> {
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
