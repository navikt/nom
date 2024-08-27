package no.nav.nom.apps.admin.events.orgtilknytning.eventsource;


import lombok.Getter;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Getter
@Service
public class OrgtilknytningEvtsrcRepository {
    private final List<OrgtilknytningEvent> orgtilknytningEvtsrc = new ArrayList<>();

    public void addOrgtilknytningEvent(OrgtilknytningEvent orgtilknytningEvent) {
        orgtilknytningEvtsrc.add(orgtilknytningEvent);
    }

    public void addOrgtilknytningEvents(List<OrgtilknytningEvent> orgtilknytningEvents) {
        orgtilknytningEvtsrc.addAll(orgtilknytningEvents);
    }

    public int count() {
        return orgtilknytningEvtsrc.size();
    }
}
