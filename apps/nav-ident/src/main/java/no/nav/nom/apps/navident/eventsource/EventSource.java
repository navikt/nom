package no.nav.nom.apps.navident.eventsource;

import no.nav.nom.apps.navident.domain.Navident;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EventSource {

    @Autowired private Map<String, Navident> navidenter;

    // Read all events from topic


    // parseEvent as navident into domain

}
