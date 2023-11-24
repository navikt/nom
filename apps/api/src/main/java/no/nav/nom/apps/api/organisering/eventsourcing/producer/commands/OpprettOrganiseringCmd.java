package no.nav.nom.apps.api.organisering.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;
import java.util.Optional;

public record OpprettOrganiseringCmd(
    String id,
    String navn,
    String idOver,
    String navnOver,
    LocalDate gyldigFom,
    Optional<LocalDate> gyldigTom,
    InputMetadata inputMetadata
) {}
