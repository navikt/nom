package no.nav.nom.apps.api.orgenhetleder.eventsourcing.recoverybackup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.OrgEnhetLederEventProducer;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederEvent;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;

import static no.nav.nom.libs.utils.common.util.DateTimeUtil.parseStringToLocalDateTime;
import static no.nav.nom.libs.utils.common.util.StreamUtils.collectToOrderedMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgEnhetLederEventSourceRecoveryService {
    private final OrgEnhetLederEventSourceRepository repository;
    private final OrgEnhetLederEventProducer eventProducer;
    private final ObjectMapper objectMapper;
    private final LeaderElection leaderElection;

    public void replayAllEventsource(boolean dryRun) {
        if (leaderElection.isLeader()) {
            Map<String, JsonNode> alleEventsource = getAllOrgEnhetLederEventSource();
            alleEventsource.forEach((key, event) -> {
                try {
                    if (dryRun) {
                        log.info("Replaying {}: {}", event.get("type").asText(), event.asText());
                    } else {
                        OrgEnhetLederEvent orgEnhetLederEvent = objectMapper.convertValue(event, OrgEnhetLederEvent.class);
                        eventProducer.replayMessage(key, orgEnhetLederEvent);
                    }
                } catch (Exception e) {
                    log.error("Failed to replay eventsource {}, stopping whole sequence", key, e);
                    throw e;
                }
            });
        }
    }

    private Map<String, JsonNode> getAllOrgEnhetLederEventSource() {
        return repository.findAll().stream()
            .sorted(Comparator.comparing(orgEnhetLederEventSource -> parseStringToLocalDateTime(orgEnhetLederEventSource.getPayload())))
            .collect(collectToOrderedMap(OrgEnhetLederEventSource::getEventKey, OrgEnhetLederEventSource::getPayload));
    }
}
