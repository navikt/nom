package no.nav.nom.apps.api.organisering.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;

public record OpprettOrganisering (
    String agressoIdOver,
    String agressoIdUnder,
    String orgNivOver,
    String orgNivUnder,
    String navnOver,
    String navnUnder,
    LocalDate gyldigFom,
    LocalDate gyldigTom,
    InputMetadata inputMetadata
) {}
