package no.nav.nom.apps.api.clients;

import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;

public class WebClientUtils {

    public static Retry defaultRetry(Integer maxAttempts, Integer backoffDurationMillis) {
        return Retry
            .backoff(maxAttempts, Duration.ofMillis(backoffDurationMillis))
            .jitter(0.75)
            .filter(throwable -> {
                if (throwable instanceof WebClientResponseException) {
                    return ((WebClientResponseException) throwable).getStatusCode().is5xxServerError();
                }
                return false;
            })
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                Throwable throwable = retrySignal.failure();
                if (throwable instanceof WebClientResponseException) {
                    throw (WebClientResponseException) throwable;
                }
                throw new RuntimeException(throwable);
            });
    }


}
