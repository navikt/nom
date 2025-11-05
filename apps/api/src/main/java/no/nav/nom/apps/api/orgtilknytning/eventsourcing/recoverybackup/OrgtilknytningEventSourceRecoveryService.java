package no.nav.nom.apps.api.orgtilknytning.eventsourcing.recoverybackup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningEventProducer;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;

import static no.nav.nom.libs.utils.common.util.DateTimeUtil.parseStringToLocalDateTime;
import static no.nav.nom.libs.utils.common.util.StreamUtils.collectToOrderedMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgtilknytningEventSourceRecoveryService {
    private final OrgtilknytningEventSourceRepository repository;
    private final OrgtilknytningEventProducer eventProducer;
    private final ObjectMapper objectMapper;
    private final LeaderElection leaderElection;


    public void replayAllEventsources(boolean dryRun) {
        if (leaderElection.isLeader()) {
            Map<String, JsonNode> alleEventsource = getAllOrgtilknytningEventSource();
            alleEventsource.forEach((key, event) -> {
                try {
                    if (dryRun) {
                        log.info("Replaying {}: {}", event.get("type").asText(), event);
                    } else {
                        OrgtilknytningEvent orgtilknytningEvent = objectMapper.convertValue(event, OrgtilknytningEvent.class);
                        eventProducer.replayMessage(key, orgtilknytningEvent);
                    }
                } catch (Exception e) {
                    log.error("Failed to replay eventsource {}, stopping whole sequence", key, e);
                    throw e;
                }
            });
        }
    }

    private Map<String, JsonNode> getAllOrgtilknytningEventSource() {
        return repository.findAll().stream()
            .sorted(Comparator.comparing(orgtilknytningEventSource -> parseStringToLocalDateTime(orgtilknytningEventSource.getPayload())))
            .collect(collectToOrderedMap(OrgtilknytningEventSource::getEventKey, OrgtilknytningEventSource::getPayload));
    }
}
