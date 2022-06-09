package no.nav.nom.apps.navident.eventsource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.Navident;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class EventSourceService {
    private final KafkaTemplate<String, SourceEvent> eventSource;
    private final Map<String, Navident> navidenter;

    @KafkaListener(
            id = "EventSourceListener",
            topics = "${app.kafka.topic.eventsource.name}")
    public void listen(SourceEvent event) {
        // Update domain model with sourced event

    }
}
