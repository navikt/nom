package no.nav.nom.apps.api.orgenhetleder.api.app;

import lombok.Builder;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record OrgEnhetLederResponse(
    String fid,
    String navident,
    Visningsnavn visningsnavn,
    String orgEnhetId,
    String orgEnhetNavn,
    LocalDate startdato,
    LocalDate sluttdato
) {
}
