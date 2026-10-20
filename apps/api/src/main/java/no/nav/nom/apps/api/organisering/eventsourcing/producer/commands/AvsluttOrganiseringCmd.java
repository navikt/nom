package no.nav.nom.apps.api.organisering.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;
import java.util.Objects;

public record AvsluttOrganiseringCmd(
    String id,
    LocalDate gyldigTom,
    InputMetadata inputMetadata
) {
    public AvsluttOrganiseringCmd{
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
    }
}