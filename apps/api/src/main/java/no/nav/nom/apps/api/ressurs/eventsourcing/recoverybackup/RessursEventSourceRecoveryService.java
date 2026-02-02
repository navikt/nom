package no.nav.nom.apps.api.ressurs.eventsourcing.recoverybackup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursEventProducer;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Comparator;
import java.util.Map;

import static no.nav.nom.libs.utils.common.util.DateTimeUtil.parseStringToLocalDateTime;
import static no.nav.nom.libs.utils.common.util.StreamUtils.collectToOrderedMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RessursEventSourceRecoveryService {
    private final RessursEventSourceRepository repository;
    private final RessursEventProducer eventProducer;
    private final JsonMapper jsonMapper;
    private final LeaderElection leaderElection;

    public void replayAllEventsource(boolean dryRun) {
        if (leaderElection.isLeader()) {
            getAllRessursEventSource().forEach((key, event) -> {
                try {
                    if (dryRun) {
                        log.info("Replaying {}: {}", event.get("type"), event);
                    } else {
                        RessursEvent ressursEvent = jsonMapper.convertValue(event, RessursEvent.class);
                        eventProducer.replayMessage(key, ressursEvent);
                    }
                } catch (Exception e) {
                    log.error("Failed to replay eventsource {}, stopping whole sequence", key, e);
                    throw e;
                }
            });
        }
    }

    private Map<String, JsonNode> getAllRessursEventSource() {
        return repository.findAll().stream()
            .sorted(Comparator.comparing(orgtilknytningEventSource -> parseStringToLocalDateTime(orgtilknytningEventSource.getPayload())))
            .collect(collectToOrderedMap(RessursEventSource::getEventKey, RessursEventSource::getPayload));
    }
}
