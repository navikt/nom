package no.nav.nom.apps.navident.eventsource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.navident.common.cache.NeedClientTokenCache;
import no.nav.nom.apps.navident.rnrservices.Need;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventSourceProducer {
    private final KafkaTemplate<String, SourceEvent> producer;
    private final SourceEventRepository sourceEventRepository;
    private final NeedClientTokenCache cache;

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

            // Since this is async from request there is no SecurityContext.Authentication present.
            // Try to populate this with token from cache
            try {
                Jwt jwt = cache.getJwtFromCacheToken(need.getUuid());
                if (jwt != null) SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
            } catch (RuntimeException e) {
                log.error("Could not set JWT Authentication on SecurityContext", e);
            }

            val persistedSourceEvent = sourceEventRepository.save(sourceEvent);

            producer.send(eventSourceTopic, persistedSourceEvent);
        }
    }
}
