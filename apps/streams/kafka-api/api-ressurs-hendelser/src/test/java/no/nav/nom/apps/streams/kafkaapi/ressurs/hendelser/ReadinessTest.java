package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;

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
    public void readinessSettlesOnUp() {
        Awaitility.await()
            .atMost(Duration.ofSeconds(15))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> restTestClient.get()
                .uri("/internal/health/readiness")
                .exchange()
                .expectStatus()
                .is2xxSuccessful());
    }
}
