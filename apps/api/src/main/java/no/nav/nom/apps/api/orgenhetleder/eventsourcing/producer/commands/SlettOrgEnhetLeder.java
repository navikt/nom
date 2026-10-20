package no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands;

import lombok.Builder;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.util.Objects;

@Builder(toBuilder = true)
public record SlettOrgEnhetLeder(
    String fid,
    String orgEnhetId,
    InputMetadata inputMetadata
) {
    public SlettOrgEnhetLeder{
        Objects.requireNonNull(orgEnhetId, "orgEnhetId must not be null");
        if (orgEnhetId.isBlank()) {
            throw new IllegalArgumentException("orgEnhetId cannot be blank");
        }
    }
}
