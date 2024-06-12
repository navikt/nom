package no.nav.nom.apps.admin;

import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RessursEvtsrcRepository {
    private final List<RessursEvent> ressursEvtsrc = new ArrayList<>();

    public void addRessursEvent(RessursEvent ressursEvent) {
        ressursEvtsrc.add(ressursEvent);
    }

    public void addRessursEvents(List<RessursEvent> ressursEvents) {
        ressursEvtsrc.addAll(ressursEvents);
    }

    public int count() {
        return ressursEvtsrc.size();
    }
}
