package no.nav.nom.apps.api.orgenhet.eventsourcing.recoverybackup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetEventProducer;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Map;

import static no.nav.nom.libs.utils.common.util.DateTimeUtil.parseStringToLocalDateTime;
import static no.nav.nom.libs.utils.common.util.StreamUtils.collectToOrderedMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgEnhetEventSourceRecoveryService {
    private final OrgEnhetEventSourceRepository repository;
    private final OrgEnhetEventProducer orgEnhetEventProducer;
    private final ObjectMapper objectMapper;
    private final LeaderElection leaderElection;

    @Transactional
    public void replayAllEventsource(boolean dryRun) {
        if (leaderElection.isLeader()) {
            Map<String, JsonNode> alleEventsource = getAllOrgEnhetEventSource();
            alleEventsource.forEach((key, event) -> {
                try {
                    if (dryRun) {
                        log.info("Replaying {}: {}", event.get("type"), event.asText());
                    } else {
                        OrgEnhetEvent orgEnhetOpprettetEventV1 = objectMapper.convertValue(event, OrgEnhetEvent.class);
                        orgEnhetEventProducer.replayMessage(key, orgEnhetOpprettetEventV1);
                    }
                } catch (Exception e) {
                    log.error("Failed to replay eventsource {}, stopping whole sequence", key, e);
                    throw e;
                }
            });
        }

    }

    private Map<String, JsonNode> getAllOrgEnhetEventSource() {
        return repository.findAll().stream()
            .sorted(Comparator.comparing(orgEnhetEventSource -> parseStringToLocalDateTime(orgEnhetEventSource.getPayload())))
            .collect(collectToOrderedMap(OrgEnhetEventSource::getEventKey, OrgEnhetEventSource::getPayload));
    }
}
