package no.nav.nom.apps.admin.webclient;

import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;

public class WebClientUtils {

    public static Retry defaultRetry = Retry
        .backoff(3, Duration.ofMillis(1))
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
