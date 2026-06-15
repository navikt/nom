package no.nav.nom.apps.admin.events.organisering.eventsource;

import lombok.Getter;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Getter
@Service
public class OrganiseringEvtsrcRepository {
    private final List<OrganiseringEvent> organiseringEvtsrc = new ArrayList<>();

    public void addOrganiseringEvents(List<OrganiseringEvent> organiseringEvents) {
        organiseringEvtsrc.addAll(organiseringEvents);
    }

    public int count() {
        return organiseringEvtsrc.size();
    }
}
