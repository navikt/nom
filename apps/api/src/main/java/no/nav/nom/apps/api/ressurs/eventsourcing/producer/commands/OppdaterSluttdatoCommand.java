package no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;
import java.util.Optional;

public record OppdaterSluttdatoCommand(
    String fid,
    Optional<LocalDate> sluttdato,
    InputMetadata inputMetadata
) implements RessursCommand{
}
