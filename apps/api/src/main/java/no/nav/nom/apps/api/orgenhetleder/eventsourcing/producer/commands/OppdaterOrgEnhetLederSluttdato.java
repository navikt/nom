package no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands;

import lombok.Builder;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record OppdaterOrgEnhetLederSluttdato(
    LocalDate sluttdato,
    InputMetadata inputMetadata
) {
}
