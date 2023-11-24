package no.nav.nom.apps.api.organisering.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;

public record AvsluttOrganiseringCmd(
    LocalDate gyldigTom,
    InputMetadata inputMetadata
) {}