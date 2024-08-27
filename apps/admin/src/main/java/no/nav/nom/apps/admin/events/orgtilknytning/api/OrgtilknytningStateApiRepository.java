package no.nav.nom.apps.admin.events.orgtilknytning.api;

import lombok.Getter;
import no.nav.nom.apps.admin.dto.orgtilknytning.OrgtilknytningStateDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Getter
@Service
public class OrgtilknytningStateApiRepository {
    private final List<OrgtilknytningStateDTO> orgtilknytningApiState = new ArrayList<>();

    public void addOrgtilknytningApiStateEvent(OrgtilknytningStateDTO orgtilknytningEvent) {
        orgtilknytningApiState.add(orgtilknytningEvent);
    }

    public void addOrgtilknytningApiStateEvents(List<OrgtilknytningStateDTO> orgtilknytningEvents) {
        orgtilknytningApiState.addAll(orgtilknytningEvents);
    }

    public int count() {
        return orgtilknytningApiState.size();
    }
}
