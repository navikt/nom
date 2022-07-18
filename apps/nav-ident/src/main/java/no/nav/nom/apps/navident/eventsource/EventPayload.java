package no.nav.nom.apps.navident.eventsource;

import lombok.Builder;
import lombok.Data;
import no.nav.nom.apps.navident.domain.IdentType;

@Builder(toBuilder = true)
@Data
public class EventPayload {
    String navident;
    String personid;
    IdentType identType;
}
