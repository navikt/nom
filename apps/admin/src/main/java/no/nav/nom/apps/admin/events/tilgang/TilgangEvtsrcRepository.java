package no.nav.nom.apps.admin.events.tilgang;

import lombok.Getter;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Getter
@Service
public class TilgangEvtsrcRepository {
    private final List<AuthorizationEvent> tilgangEvtsrc = new ArrayList<>();

    public void addTilgangEvent(AuthorizationEvent tilgangEvent) {
        tilgangEvtsrc.add(tilgangEvent);
    }

    public void addTilgangEvents(List<AuthorizationEvent> tilgangEvents) {
        tilgangEvtsrc.addAll(tilgangEvents);
    }

    public int count() {
        return tilgangEvtsrc.size();
    }
}
