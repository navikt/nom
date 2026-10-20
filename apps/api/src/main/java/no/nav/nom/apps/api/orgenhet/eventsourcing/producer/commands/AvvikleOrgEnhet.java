package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;
import java.util.Objects;

public record AvvikleOrgEnhet(
        String id,
        LocalDate gyldigTom,
        InputMetadata inputMetadata
) {
    public AvvikleOrgEnhet{
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
    }
}
