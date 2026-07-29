package no.nav.nom.apps.streams.internal.organisering;

import no.nav.nom.libs.utils.common.unleash.UnleashAutoconfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;

@EmbeddedKafka(topics = {
    "org.nom.intern-organisering-state",
    "org.nom.organisering-eventsource"
})
@SpringBootTest
@ActiveProfiles("test")
public class ReadinessTest {

    private final RestTestClient restTestClient;

    @MockitoBean
    public UnleashAutoconfiguration.UnleashHealthSubscriber unleashHealthSubscriber;

    public ReadinessTest(WebApplicationContext webApplicationContext ) {

        this.restTestClient = RestTestClient.bindToApplicationContext(webApplicationContext).build();
    }

    @Test
    public void contextRunsKafka() throws InterruptedException {
        Mockito.when(unleashHealthSubscriber.health(Mockito.anyBoolean())).thenReturn(Health.down().build(), Health.up().withDetail("service","unleashHealthCheckSubscriber").build());

        for (int i = 0; i < 12; i++) {
            var readinessResult = restTestClient.get().uri("/internal/health/readiness").exchange().returnResult(String.class);
            var readyImmediately = i == 0 && !readinessResult.getStatus().isError();
            if (readyImmediately) {
                throw new IllegalStateException("Unexpected: Streams application was ready immediately after startup");
            }
            var reachedReadyState = readinessResult.getStatus().is2xxSuccessful();
            if (reachedReadyState) {
                return;
            } else {
                Assertions.assertThat(readinessResult.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                Assertions.assertThat(readinessResult.getResponseBody()).contains(KafkaReadiness.class.getSimpleName());
            }
            Thread.sleep(50 * i);
        }
        throw new IllegalStateException("Streams application was not ready by expected time threshold.");
    }

    @Test
    public void contextRunsUnleash() throws InterruptedException {
        Mockito.when(unleashHealthSubscriber.health(Mockito.anyBoolean())).thenReturn(Health.down().build(), Health.up().withDetail("service","unleashHealthCheckSubscriber").build());

        for (int i = 0; i < 12; i++) {
            var readinessResult = restTestClient.get().uri("/internal/health/readiness").exchange().returnResult(String.class);
            var readyImmediately = i == 0 && !readinessResult.getStatus().isError();
            if (readyImmediately) {
                throw new IllegalStateException("Unexpected: Streams application was ready immediately after startup");
            }
            var reachedReadyState = readinessResult.getStatus().is2xxSuccessful();
            if (reachedReadyState) {
                return;
            } else {
                Assertions.assertThat(readinessResult.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                Assertions.assertThat(readinessResult.getResponseBody()).contains("unleashHealthCheckSubscriber");
            }
            Thread.sleep(50 * i);
        }
        throw new IllegalStateException("Streams application was not ready by expected time threshold.");
    }
}
