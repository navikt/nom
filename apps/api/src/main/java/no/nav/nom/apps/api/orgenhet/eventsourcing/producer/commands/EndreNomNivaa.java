package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.util.Optional;

public record EndreNomNivaa(
    Optional<NomNivaa> nomNivaa,
    InputMetadata inputMetadata

) {
}
