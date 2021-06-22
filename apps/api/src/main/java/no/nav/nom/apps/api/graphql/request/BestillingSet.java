package no.nav.nom.apps.api.graphql.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.nom.apps.api.graphql.request.dto.Bestilling;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BestillingSet {

    UUID bestillingSetId;
    List<Bestilling> bestillinger;

    String beskrivelse;
    LocalDateTime sistEndret;
    LocalDateTime activateTime;

}
