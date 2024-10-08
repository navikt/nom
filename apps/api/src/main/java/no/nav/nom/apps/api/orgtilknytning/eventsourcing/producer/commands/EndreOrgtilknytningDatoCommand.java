package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands;

import lombok.Builder;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;
import java.util.Optional;

@Builder(toBuilder = true)
public record EndreOrgtilknytningDatoCommand (
    String fid,
    LocalDate gyldigFom,
    Optional<LocalDate> gyldigTom,
    InputMetadata inputMetadata
) implements OrgtilknytningCommand {}
