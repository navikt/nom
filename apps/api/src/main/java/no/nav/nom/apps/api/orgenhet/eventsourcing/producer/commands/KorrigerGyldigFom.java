package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;

public record KorrigerGyldigFom(
    LocalDate gyldigFom,
    InputMetadata inputMetadata
) {
}
