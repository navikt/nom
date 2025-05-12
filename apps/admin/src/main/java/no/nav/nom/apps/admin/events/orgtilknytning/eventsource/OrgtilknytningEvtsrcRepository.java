package no.nav.nom.apps.admin.events.orgtilknytning.eventsource;


import lombok.Getter;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Service
public class OrgtilknytningEvtsrcRepository {
    private final List<OrgtilknytningEvent> orgtilknytningEvtsrc = new ArrayList<>();
    private final Map<String, List<String>> navidentToFidMap = new HashMap<>();

    public void addOrgtilknytningEvent(OrgtilknytningEvent orgtilknytningEvent) {
        orgtilknytningEvtsrc.add(orgtilknytningEvent);
        if (orgtilknytningEvent instanceof OrgtilknytningOpprettetEventV1 v1) {
            var navident = v1.getOrgtilknytningOpprettetEventData().navident();
            var fid = v1.getFid();
            if (navidentToFidMap.containsKey(navident)) {
                navidentToFidMap.get(navident).add(fid);
            } else {
                List<String> fidList = new ArrayList<>();
                fidList.add(fid);
                navidentToFidMap.put(navident, fidList);
            }
        }
    }

    public void addOrgtilknytningEvents(List<OrgtilknytningEvent> orgtilknytningEvents) {
        orgtilknytningEvtsrc.addAll(orgtilknytningEvents);
    }

    public int count() {
        return orgtilknytningEvtsrc.size();
    }
}
