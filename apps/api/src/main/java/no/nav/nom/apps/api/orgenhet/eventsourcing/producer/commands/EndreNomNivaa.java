package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

public record EndreNomNivaa(
    NomNivaa nomNivaa,
    InputMetadata inputMetadata

) {
}
