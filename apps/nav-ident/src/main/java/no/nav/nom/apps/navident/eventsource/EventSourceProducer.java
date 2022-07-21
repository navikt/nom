package no.nav.nom.apps.navident.eventsource;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.navident.rnrservices.Need;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventSourceProducer {

    private final KafkaTemplate<String, SourceEvent> producer;
    private final SourceEventRepository sourceEventRepository;

    @Value("${app.kafka.topic.eventsource.name}")
    private String eventSourceTopic;

    @KafkaListener(id = "EventSourceProducer", topics = "${app.kafka.topic.rnr.name}")
    public void listen(Need need) {
        if (need.isResponsProdusentVisited() && need.getRespons() != null && List.of(HttpStatus.OK, HttpStatus.CREATED).contains(need.getRespons().getHttpStatus())) {
            val responseBody = need.getRespons().getBody();
            val sourceEvent = SourceEvent.builder()
                    .event(EventType.OPPRETT)
                    .payload(
                            EventPayload.builder()
                                    .navident(responseBody.getNavident())
                                    .identType(need.getIdentType())
                                    .personid(need.getPersonid())
                                    .build()
                    ).build();
            val persistedSourceEvent = sourceEventRepository.save(sourceEvent);

            producer.send(eventSourceTopic, persistedSourceEvent);
        }
    }
}
