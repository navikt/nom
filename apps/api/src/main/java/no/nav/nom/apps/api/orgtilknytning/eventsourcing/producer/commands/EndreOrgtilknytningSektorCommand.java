package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands;

import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;

@Builder(toBuilder = true)
public record EndreOrgtilknytningSektorCommand(
    String fid,
    String navident,
    String orgenhetId,
    Sektor sektor,
    InputMetadata inputMetadata,
    OrgtilknytningEvent.AdditionalAgressoData additionalAgressoData
) implements OrgtilknytningCommand {
}
