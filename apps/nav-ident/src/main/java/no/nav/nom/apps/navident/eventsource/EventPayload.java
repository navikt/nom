package no.nav.nom.apps.navident.eventsource;

import lombok.Builder;
import lombok.Data;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.domain.PersonidType;

@Builder(toBuilder = true)
@Data
public class EventPayload {
    String navident;
    String personid;
    PersonidType personidtype;
    IdentType identType;
}
