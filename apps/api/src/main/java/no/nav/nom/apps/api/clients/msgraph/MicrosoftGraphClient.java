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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
            logger.info("Attempting to send email: to({}), cc({}), url: {}",
                    emailMessage.message().toRecipients(),
                    emailMessage.message().ccRecipients(),
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
        String decodedUri = decodeUri(uri);
        logger.debug("Decoded URI: {}", decodedUri);
        return webClient.get()
                .uri(decodedUri)
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
    
    private String decodeUri(String url) {
        return URLDecoder.decode(url, StandardCharsets.UTF_8);
    }

    private Retry defaultRetry(Integer maxAttempts, Integer backoffDurationMillis) {
        return Retry.backoff(maxAttempts, Duration.ofMillis(backoffDurationMillis))
            .jitter(0.75)
            .doBeforeRetryAsync(signal -> Mono.delay(calculateDelay(signal.failure())).then())
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                Throwable throwable = retrySignal.failure();
                if (throwable instanceof WebClientResponseException) {
                    throw (WebClientResponseException) throwable;
                }
                throw new RuntimeException(throwable);
            });
    }

    private static Duration calculateDelay(Throwable failure) {
        if (!(failure instanceof WebClientResponseException.TooManyRequests)) {
            logger.info("Failure is not a TooManyRequests exception, defaulting to 0 seconds delay");
            return Duration.ofSeconds(0); // Default delay if not a ServiceUnavailable exception
        }

        logger.info("Calculating delay based on Retry-After header for TooManyRequests exception");

        var retryAfterHeader = ((WebClientResponseException.TooManyRequests) failure).getHeaders().get("Retry-After");
        if (retryAfterHeader == null || retryAfterHeader.isEmpty()) {
            logger.warn("No Retry-After header found in response, defaulting to 0 seconds delay");
            return Duration.ofSeconds(0); // Default delay if no header is present
        }
        String headerValue = retryAfterHeader.getFirst();
        try {
            var durationInSeconds = Integer.parseInt(headerValue);
            logger.info("Got Retry-After header value: {} seconds", durationInSeconds);
            return Duration.ofSeconds(durationInSeconds);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse Retry-After header value as Int: {}", headerValue, e);
        }
        return Duration.ofSeconds(0); // Default delay if parsing fails
    }
}
