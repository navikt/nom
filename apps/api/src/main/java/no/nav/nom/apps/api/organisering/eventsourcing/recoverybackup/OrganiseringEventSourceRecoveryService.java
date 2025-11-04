package no.nav.nom.apps.api.organisering.eventsourcing.recoverybackup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventProducer;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;

import static no.nav.nom.libs.utils.common.util.DateTimeUtil.parseStringToLocalDateTime;
import static no.nav.nom.libs.utils.common.util.StreamUtils.collectToOrderedMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganiseringEventSourceRecoveryService {
    private final OrganiseringEventSourceRepository repository;
    private final OrganiseringEventProducer eventProducer;
    private final ObjectMapper objectMapper;
    private final LeaderElection leaderElection;

    public void replayAllEventsource(boolean dryRun) {
        if (leaderElection.isLeader()) {
            getAllOrganiseringEventSource().forEach((key, event) -> {
                try {
                    if (dryRun) {
                        log.info("Replaying {}: {}", event.get("type").asText(), event.asText());
                    } else {
                        OrganiseringEvent organiseringEvent = objectMapper.convertValue(event, OrganiseringEvent.class);
                        eventProducer.replayMessage(key, organiseringEvent);
                    }
                } catch (Exception e) {
                    log.error("Failed to replay eventsource {}, stopping whole sequence", key, e);
                    throw e;
                }
            });
        }
    }

    private Map<String, JsonNode> getAllOrganiseringEventSource() {
        return repository.findAll()
            .stream()
            .sorted(Comparator.comparing(o -> parseStringToLocalDateTime(o.getPayload())))
            .collect(collectToOrderedMap(OrganiseringEventSource::getEventKey, OrganiseringEventSource::getPayload));
    }
}
