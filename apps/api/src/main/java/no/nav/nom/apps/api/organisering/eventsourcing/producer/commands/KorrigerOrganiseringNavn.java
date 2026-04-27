package no.nav.nom.apps.api.organisering.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;

public record KorrigerOrganiseringNavn(
    String id,
    String navn,
    InputMetadata inputMetadata
) {
}
