package no.nav.nom.apps.api.clients.msgraph;

import no.nav.nom.apps.api.common.AppProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
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

    private final RestClient restClient;
    private final AppProperties.MicrosoftGraph msGraphClientProperties;

    public MicrosoftGraphClient(
            @Qualifier("authorizedClientManager") OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
            AppProperties appProperties,
            RestClient.Builder builder
    ) {
        this.msGraphClientProperties = appProperties.clients().microsoftGraph();

        OAuth2ClientHttpRequestInterceptor oauth2Interceptor = new OAuth2ClientHttpRequestInterceptor(oAuth2AuthorizedClientManager);
        oauth2Interceptor.setClientRegistrationIdResolver(_ -> "email");

        this.restClient = builder
                .requestInterceptor(oauth2Interceptor)
                .baseUrl(msGraphClientProperties.baseUrl())
                .build();
    }

    public Mono<Void> sendEmail(EmailMessage emailMessage) {
        if (msGraphClientProperties.sendEmail()) {
            logger.info("Attempting to send email: to({}), cc({}), bcc({}), url: {}",
                    emailMessage.message().toRecipients(),
                    emailMessage.message().ccRecipients(),
                    emailMessage.message().bccRecipients(),
                    msGraphClientProperties.baseUrl() + EMAIL_URI_PATH);
        return Mono.<ODataResponse<EntraIdUser>>fromCallable(() -> {
                restClient.post()
                        .uri(EMAIL_URI_PATH)
                        .body(emailMessage)
                        .retrieve()
                        .toBodilessEntity();
                return null;
            })
            .retryWhen(defaultRetry(msGraphClientProperties.maxAttempts(), msGraphClientProperties.backoffDurationMillis()))
            .doOnError(Exception.class, (Exception exception) -> {
                String errorMsg = exception.getMessage();
                if (exception instanceof RestClientResponseException e) {
                    errorMsg = String.format(
                            "POST %s resulted in status %s with body %s",
                            EMAIL_URI_PATH,
                            e.getStatusCode().value(),
                            e.getResponseBodyAsString()
                    );
                }
                logger.error("Sending email failed: {}", errorMsg);
                throw new EmailSendingUnforeseenException(errorMsg);
            })
            .then();
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

        final ODataResponse<EntraIdUser> empty = ODataResponse.<EntraIdUser>builder().value(Collections.emptyList()).build();

        return Mono.fromCallable(() -> {
            try {
                ODataResponse<EntraIdUser> response = restClient.get()
                        .uri(decodedUri)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});
                return response != null ? response : empty;
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().is4xxClientError() || e.getStatusCode().is5xxServerError()) {
                    ODataError oDataError = e.getResponseBodyAs(ODataError.class);
                    if (oDataError != null && oDataError.error() != null) {
                        logger.warn("GET failed for URI '{}': Response code: '{}' Error code: '{}' Error message: '{}'",
                                uri, e.getStatusCode(), oDataError.error().code(), oDataError.error().message());
                    } else {
                        logger.warn("GET failed for URI '{}': Response code: '{}'", uri, e.getStatusCode());
                    }
                    return empty;
                }
                throw e;
            }
        })
        .retryWhen(defaultRetry(msGraphClientProperties.maxAttempts(), msGraphClientProperties.backoffDurationMillis()))
        .doOnError(Exception.class, exception ->
                logger.warn("GET failed for URI '{}': {}", uri, exception.getMessage(), exception))
        .onErrorReturn(empty)
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
                if (throwable instanceof RestClientResponseException) {
                    throw (RestClientResponseException) throwable;
                }
                throw new RuntimeException(throwable);
            });
    }

    private static Duration calculateDelay(Throwable failure) {
        if (!(failure instanceof RestClientResponseException e) || e.getStatusCode().value() != 429) {
            logger.info("Failure is not a TooManyRequests exception, defaulting to 0 seconds delay");
            return Duration.ofSeconds(0);
        }

        logger.info("Calculating delay based on Retry-After header for TooManyRequests exception");

        var retryAfterHeader = e.getResponseHeaders() != null ? e.getResponseHeaders().get("Retry-After") : null;
        if (retryAfterHeader == null || retryAfterHeader.isEmpty()) {
            logger.warn("No Retry-After header found in response, defaulting to 0 seconds delay");
            return Duration.ofSeconds(0);
        }
        String headerValue = retryAfterHeader.getFirst();
        try {
            var durationInSeconds = Integer.parseInt(headerValue);
            logger.info("Got Retry-After header value: {} seconds", durationInSeconds);
            return Duration.ofSeconds(durationInSeconds);
        } catch (NumberFormatException nfe) {
            logger.warn("Failed to parse Retry-After header value as Int: {}", headerValue, nfe);
        }
        return Duration.ofSeconds(0);
    }
}
