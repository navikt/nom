package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands;

import lombok.Builder;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.util.Optional;

@Builder(toBuilder = true)
public record EndreTilknytningtypeCommand(
    String fid,
    Optional<Tilknytningtype> tilknytningtype,
    InputMetadata inputMetadata
) {}
