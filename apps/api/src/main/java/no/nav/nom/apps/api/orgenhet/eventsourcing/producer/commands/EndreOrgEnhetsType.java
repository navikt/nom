package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.util.Optional;

public record EndreOrgEnhetsType(
    Optional<OrgEnhetsType> orgEnhetsType,
    InputMetadata inputMetadata

) {
}
