package no.nav.nom.apps.api.graphql.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestSet {

    UUID requestSetId;
    List<Request> requests;

    String beskrivelse;
    LocalDateTime sistEndret;
    LocalDateTime activateTime;

    public List<UUID> getRequestIds() {
        return requests.stream().map(Request::getRequestId).toList();
    }
}
