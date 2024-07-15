package no.nav.nom.apps.admin.events.orgenhet.api;

import lombok.Getter;
import no.nav.nom.apps.admin.dto.orgenhet.OrgEnhetStateDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Getter
@Service
public class OrgEnhetStateApiRepository {
    private final List<OrgEnhetStateDTO> orgtilknytningApiState = new ArrayList<>();

    public void addOrgtilknytningApiStateEvent(OrgEnhetStateDTO orgtilknytningEvent) {
        orgtilknytningApiState.add(orgtilknytningEvent);
    }

    public void addOrgtilknytningApiStateEvents(List<OrgEnhetStateDTO> orgtilknytningEvents) {
        orgtilknytningApiState.addAll(orgtilknytningEvents);
    }

    public int count() {
        return orgtilknytningApiState.size();
    }
}
