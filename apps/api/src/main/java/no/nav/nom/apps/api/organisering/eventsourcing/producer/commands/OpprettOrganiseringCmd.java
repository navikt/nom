package no.nav.nom.apps.api.organisering.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;

public record OpprettOrganiseringCmd(
    String agressoIdOver,
    String agressoId,
    String orgNivOver,
    String orgNiv,
    String navnOver,
    String navn,
    LocalDate gyldigFom,
    LocalDate gyldigTom,
    InputMetadata inputMetadata
) {}
