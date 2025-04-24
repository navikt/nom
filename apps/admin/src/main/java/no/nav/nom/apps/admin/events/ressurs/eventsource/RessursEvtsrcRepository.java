package no.nav.nom.apps.admin.events.ressurs.eventsource;

import lombok.Getter;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Service
public class RessursEvtsrcRepository {
    private final List<RessursEvent> ressursEvtsrc = new ArrayList<>();
    private final Map<String, List<String>> navidentToFidMap = new HashMap<>();

    public void addRessursEvent(RessursEvent ressursEvent) {
        ressursEvtsrc.add(ressursEvent);
    }

    public void addRessursEvents(List<RessursEvent> ressursEvents) {
        ressursEvtsrc.addAll(ressursEvents);
        ressursEvtsrc.forEach(ressursEvent -> {
            if (ressursEvent instanceof RessursOpprettetEventV1 v1) {
                v1.getRessursOpprettetAdditionalData().navident().ifPresentOrElse(navident -> {
                    if (navidentToFidMap.containsKey(navident)) {
                        navidentToFidMap.get(navident).add(v1.getFid());
                    } else {
                        List<String> fidList = new ArrayList<>();
                        fidList.add(v1.getFid());
                        navidentToFidMap.put(navident, fidList);
                    }
                },
                () -> {
                    // Do nothing if navident is not present
                });
            }
        });
    }

    public int count() {
        return ressursEvtsrc.size();
    }
}
