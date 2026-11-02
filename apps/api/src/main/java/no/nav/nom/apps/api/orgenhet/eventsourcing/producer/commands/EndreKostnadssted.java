package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.util.Objects;

public record EndreKostnadssted(
    String id,
    String kostnadsstedId,
    InputMetadata inputMetadata
) {
    public EndreKostnadssted {
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
    }
}