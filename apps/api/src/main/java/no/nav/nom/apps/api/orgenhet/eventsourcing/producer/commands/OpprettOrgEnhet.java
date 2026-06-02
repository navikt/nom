package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;

public record OpprettOrgEnhet(
        String id,
        String navn,
        LocalDate gyldigFom,
        String overordnetOrgEnhetFid,
        String lederRessursFid,
        String tilgangsenhetId,
        InputMetadata inputMetadata
) {}
