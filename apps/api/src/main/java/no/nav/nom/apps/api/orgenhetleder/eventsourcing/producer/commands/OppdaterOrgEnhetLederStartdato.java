package no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands;

import lombok.Builder;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;
import java.util.Objects;

@Builder(toBuilder = true)
public record OppdaterOrgEnhetLederStartdato(
    String orgEnhetId,
    LocalDate startdato,
    InputMetadata inputMetadata
) {
    public OppdaterOrgEnhetLederStartdato{
        Objects.requireNonNull(orgEnhetId, "orgEnhetId must not be null");
        if (orgEnhetId.isBlank()) {
            throw new IllegalArgumentException("orgEnhetId cannot be blank");
        }
    }
}
