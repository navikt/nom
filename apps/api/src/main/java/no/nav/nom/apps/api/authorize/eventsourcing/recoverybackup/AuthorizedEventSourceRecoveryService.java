package no.nav.nom.apps.api.authorize.eventsourcing.recoverybackup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.AuthorizeEventProducer;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;

import static no.nav.nom.libs.utils.common.util.DateTimeUtil.parseStringToLocalDateTime;
import static no.nav.nom.libs.utils.common.util.StreamUtils.collectToOrderedMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizedEventSourceRecoveryService {
    private final AuthorizeEventSourceRepository repository;
    private final AuthorizeEventProducer eventProducer;
    private final ObjectMapper objectMapper;
    private final LeaderElection leaderElection;

    public void replayAllEventsource(boolean dryRun) {
        if (leaderElection.isLeader()) {
            getAllAuthorizedEventSource().forEach((key, event) -> {
                try {
                    if (dryRun) {
                        log.info("Replaying {}: {}", event.get("type").asText(), event.asText());
                    } else {
                        AuthorizationEvent authorizationEvent = objectMapper.convertValue(event, AuthorizationEvent.class);
                        eventProducer.replayMessage(key, authorizationEvent);
                    }
                } catch (Exception e) {
                    log.error("Failed to replay eventsource {}, stopping whole sequence", key, e);
                    throw e;
                }
            });
        }
    }

    private Map<String, JsonNode> getAllAuthorizedEventSource() {
        return repository.findAll()
            .stream()
            .sorted(Comparator.comparing(o -> parseStringToLocalDateTime(o.getPayload())))
            .collect(collectToOrderedMap(AuthorizeEventSource::getEventKey, AuthorizeEventSource::getPayload));
    }
}
