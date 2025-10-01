package no.nav.nom.apps.admin.events.orgenhet.eventsource;

import lombok.Getter;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Getter
@Service
public class OrgenhetEvtsrcRepository {
    private final List<OrgEnhetEvent> OrgenhetEvtsrc = new ArrayList<>();

    public void addOrgEnhetEvents(List<OrgEnhetEvent> OrgEnhetEvents) {
        OrgenhetEvtsrc.addAll(OrgEnhetEvents);
    }

    public int count() {
        return OrgenhetEvtsrc.size();
    }
}
