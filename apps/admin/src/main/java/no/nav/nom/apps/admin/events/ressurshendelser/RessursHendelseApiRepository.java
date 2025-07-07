package no.nav.nom.apps.admin.events.ressurshendelser;

import lombok.Getter;
import no.nav.nom.apps.admin.dto.ressurs.RessursHendelseKafkaDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Getter
public class RessursHendelseApiRepository {
    private final List<RessursHendelseKafkaDTO> ressursHendelseApi = new ArrayList<>();

    public void addRessursHendelseApiEvents(List<RessursHendelseKafkaDTO> ressursHendelseEvents) {
        ressursHendelseApi.addAll(ressursHendelseEvents);
    }

    public int count() {
        return ressursHendelseApi.size();
    }
}
