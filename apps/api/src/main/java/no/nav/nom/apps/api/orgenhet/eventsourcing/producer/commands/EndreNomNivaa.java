package no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.OrgEnhetsKategori;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.util.Optional;

public record EndreNomNivaa(
    Optional<OrgEnhetsKategori> nomNivaa,
    InputMetadata inputMetadata

) {
}
