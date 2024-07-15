package no.nav.nom.apps.admin.events.hendelser;

import lombok.Getter;
import no.nav.nom.apps.admin.dto.hendelse.HendelseDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Getter
@Service
public class HendelserApiStateRepository {
    private final List<HendelseDTO> hendelserApiState = new ArrayList<>();

    public void addHendelseApiStateEvent(HendelseDTO hendelseEvent) {
        hendelserApiState.add(hendelseEvent);
    }

    public void addHendelseApiStateEvents(List<HendelseDTO> hendelseEvents) {
        hendelserApiState.addAll(hendelseEvents);
    }

    public int count() {
        return hendelserApiState.size();
    }
}
