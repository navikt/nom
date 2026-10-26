package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.util.Objects;

public record EndreNomNivaa(
    String id,
    NomNivaa nomNivaa,
    InputMetadata inputMetadata
) {
    public EndreNomNivaa{
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
    }
}
