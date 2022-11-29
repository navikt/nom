package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.util.Optional;

public record OpprettOrgEnhet(
        String id,
        String navn,
        Optional<String> parentOrgEnhetFid,
        Optional<String> lederRessursFid,
        InputMetadata inputMetadata
) {}
