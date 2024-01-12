package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.util.Optional;

public record EndreOrgtilknytningType(
    Optional<Tilknytningtype> orgtilknytningType,
    InputMetadata inputMetadata) {}
