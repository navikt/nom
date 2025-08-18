package no.nav.nom.apps.streams.kafkaapi.ressurs.state.v4;

import no.nav.nom.apps.streams.kafkaapi.ressurs.state.MessageMetadata;

public record OrgtilknytningExt(
    Orgtilknytning orgtilknytning,
    MessageMetadata messageMetadata
) {}
