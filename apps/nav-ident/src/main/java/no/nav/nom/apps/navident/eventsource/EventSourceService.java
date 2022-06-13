package no.nav.nom.apps.navident.eventsource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.Navident;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class EventSourceService {
    private final Map<String, Navident> navidenter;

    public final CountDownLatch latch = new CountDownLatch(1);

    @KafkaListener(
            id = "EventSourceListener",
            topics = "${app.kafka.topic.eventsource.name}")
    public void listen(SourceEvent event) {
        // Update domain model with sourced event
        log.info(String.valueOf(event));
        latch.countDown();

    }
}
