package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

public record EndreOrgEnhetsType(
    OrgEnhetsType orgEnhetsType,
    InputMetadata inputMetadata

) {
}
