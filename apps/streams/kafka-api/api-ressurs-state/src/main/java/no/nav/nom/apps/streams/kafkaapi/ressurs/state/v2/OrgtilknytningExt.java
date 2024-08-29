package no.nav.nom.apps.streams.kafkaapi.ressurs.state.v2;

import no.nav.nom.apps.streams.kafkaapi.ressurs.state.MessageMetadata;

public record OrgtilknytningExt(
    Orgtilknytning orgtilknytning,
    MessageMetadata messageMetadata
) {}
