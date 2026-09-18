package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands;

import lombok.Builder;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.util.List;

@Builder(toBuilder = true)
public record EndreFysiskeBrukerstederCommand(
    String fid,
    String navident,
    String orgEnhetId,
    String primaryFysiskBrukerstedNummer,
    List<String> additionalFysiskeBrukerstederNummer,
    String masseflyttetFraFid,
    InputMetadata inputMetadata
) implements OrgtilknytningCommand {
}
