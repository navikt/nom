package no.nav.nom.apps.admin.events.ressurs.api;

import lombok.Getter;
import no.nav.nom.apps.admin.dto.ressurs.RessursStateKafkaDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Getter
@Service
public class RessursApiStateRepository {
    private final List<RessursStateKafkaDTO> ressursStateTopic = new ArrayList<>();

    public void addRessursStateTopicEvent(RessursStateKafkaDTO ressursEvent) {
        ressursStateTopic.add(ressursEvent);
    }

    public void addRessursStateTopicEvents(List<RessursStateKafkaDTO> ressursEvents) {
        ressursStateTopic.addAll(ressursEvents);
    }

    public int count() {
        return ressursStateTopic.size();
    }
}
