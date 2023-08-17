package no.nav.nom.apps.navident.eventsource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.navident.common.LogConfig;
import no.nav.nom.apps.navident.common.cache.RnRClientTokenCache;
import no.nav.nom.apps.navident.common.cache.RnRMdcCache;
import no.nav.nom.apps.navident.rnrservices.Need;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventSourceProducer {
    private final KafkaTemplate<String, SourceEvent> producer;
    private final SourceEventRepository sourceEventRepository;
    private final RnRClientTokenCache cache;
    private final RnRMdcCache mdcCache;

    @Value("${app.kafka.topic.eventsource.name}")
    private String eventSourceTopic;

    @KafkaListener(id = "EventSourceProducer", topics = "${app.kafka.topic.rnr.name}")
    public void listen(Need need) {
        mdcCache.loadMdc(need.getUuid());

        if (need.isResponsProdusentVisited() && need.getRespons() != null && List.of(HttpStatus.OK, HttpStatus.CREATED).contains(need.getRespons().getHttpStatus())) {
            log.debug(LogConfig.SECURE_LOGS,"Producing and persisting SourceEvent for need: {}", need);
            log.debug("Producing and persisting SourceEvent for need: %s".formatted(need.maskedToString()));
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

            // Since this is async from request there is no SecurityContext.Authentication present.
            // This is needed for jpa @CreatedBy
            cache.setCachedTokedBackOnSecurityContext(need.getUuid());

            val persistedSourceEvent = sourceEventRepository.save(sourceEvent);

            producer.send(eventSourceTopic, persistedSourceEvent);

            log.debug("Persisted SourceEvent posted: %s".formatted(persistedSourceEvent.maskedToString()));

            MDC.clear();
        }
    }
}
