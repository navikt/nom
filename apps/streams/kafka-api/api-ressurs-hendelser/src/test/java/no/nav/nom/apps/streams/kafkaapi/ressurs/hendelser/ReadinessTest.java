package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;

@EmbeddedKafka(
        topics = {
                "org.nom.ressurs-eventsource",
                "org.nom.orgtilknytning-eventsource",
                "org.nom.api-ressurs-hendelse-v1"
        }
)
@SpringBootTest
@ActiveProfiles("test")
public class ReadinessTest {

    private final RestTestClient restTestClient;

    public ReadinessTest(WebApplicationContext webApplicationContext) {
        this.restTestClient = RestTestClient.bindToApplicationContext(webApplicationContext).build();
    }

    @Test
    public void readinessSettlesOnUp() throws InterruptedException {
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
                Assertions.assertThat(readinessResult.getResponseBody()).contains(KafkaReadiness.class.getSimpleName());
            }
            Thread.sleep(50 * i);
        }
        throw new IllegalStateException("Streams application was not ready by expected time threshold.");
    }
}
