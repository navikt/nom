package no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands;

import lombok.Builder;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

@Builder(toBuilder = true)
public record OpprettOrgEnhetLeder(
    String navident,
    String orgEnhetId,
    InputMetadata inputMetadata
) {
}
