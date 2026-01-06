package no.nav.nom.apps.admin.events.orgenhetleder.eventsource;

import lombok.Getter;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Getter
@Service
public class OrgenhetLederEvtsrcRepository {
    private final List<OrgEnhetLederEvent> OrgenhetLederEvtsrc = new ArrayList<>();

    public void addOrgEnhetLederEvents(List<OrgEnhetLederEvent> OrgEnhetLederEvents) {
        OrgenhetLederEvtsrc.addAll(OrgEnhetLederEvents);
    }

    public int count() {
        return OrgenhetLederEvtsrc.size();
    }
}
