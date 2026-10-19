package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;
import java.util.Objects;

public record KorrigerGyldigFom(
    String id,
    LocalDate gyldigFom,
    InputMetadata inputMetadata
) {
    public KorrigerGyldigFom{
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
    }
}
