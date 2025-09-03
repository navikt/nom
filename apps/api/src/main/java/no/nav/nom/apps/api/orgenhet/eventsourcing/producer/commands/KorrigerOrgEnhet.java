package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;

public record KorrigerOrgEnhet(
        String id,
        String navn,
        LocalDate gyldigFom,
        LocalDate gyldigTom,
        String overordnetOrgEnhetFid,
        String lederRessursFid,
        InputMetadata inputMetadata
) {}
