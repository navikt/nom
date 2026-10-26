package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.util.Objects;

public record EndreOrgEnhetsType(
    String id,
    OrgEnhetsType orgEnhetsType,
    InputMetadata inputMetadata
) {
    public EndreOrgEnhetsType{
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
    }
}
