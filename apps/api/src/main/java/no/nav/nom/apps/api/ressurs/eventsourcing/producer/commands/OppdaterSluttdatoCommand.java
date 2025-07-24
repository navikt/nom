package no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands;

import lombok.Builder;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;
import java.util.Optional;

@Builder(toBuilder = true)
public record OppdaterSluttdatoCommand(
    String fid,
    LocalDate sluttdato,
    InputMetadata inputMetadata
) implements RessursCommand{
}
