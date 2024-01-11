package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.OrgtilknytningType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.util.Optional;

public record EndreOrgtilknytningType(
    Optional<OrgtilknytningType> orgtilknytningType,
    InputMetadata inputMetadata) {}
