package no.nav.nom.apps.admin.events.ressurs.api;

import lombok.Getter;
import no.nav.nom.apps.admin.dto.ressurs.RessursStateKafkaDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Service
public class RessursApiStateV4Repository {
    private final List<RessursStateKafkaDTO> ressursStateTopic = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, RessursStateKafkaDTO> latestRessursStatePerNavident = new ConcurrentHashMap<>();
    private final Map<String, RessursStateKafkaDTO> latestRessursStatePerFid = new ConcurrentHashMap<>();

    public void addRessursStateTopicEvent(RessursStateKafkaDTO ressursEvent) {
        ressursStateTopic.add(ressursEvent);
    }

    public void addRessursStateTopicEvents(List<RessursStateKafkaDTO> ressursEvents) {
        ressursStateTopic.addAll(ressursEvents);
        ressursEvents.forEach(r -> latestRessursStatePerNavident.put(r.navident(), r));
        ressursEvents.forEach(r -> latestRessursStatePerFid.put(r.key(), r));
    }

    public int count() {
        return ressursStateTopic.size();
    }

    public int countDistinctNavident() { return latestRessursStatePerNavident.size(); }
}
