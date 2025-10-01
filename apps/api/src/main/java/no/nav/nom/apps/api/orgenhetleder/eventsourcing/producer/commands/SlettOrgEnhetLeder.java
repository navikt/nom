package no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands;

import lombok.Builder;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

@Builder(toBuilder = true)
public record SlettOrgEnhetLeder(
    String fid,
    InputMetadata inputMetadata
) {
}
