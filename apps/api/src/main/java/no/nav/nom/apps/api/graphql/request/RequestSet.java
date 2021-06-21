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
public class RequestSet {

    UUID requestSetId;
    List<Bestilling> bestillinger;

    String beskrivelse;
    LocalDateTime sistEndret;
    LocalDateTime activateTime;

    public List<UUID> getRequestIds() {
        return bestillinger.stream().map(Bestilling::getRequestId).toList();
    }
}
