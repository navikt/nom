package no.nav.nom.apps.streams.kafkaapi.ressurs.state.v4;

import no.nav.nom.apps.streams.kafkaapi.ressurs.state.MessageMetadata;

public record RessursStateExt(
    RessursState ressursState,
    MessageMetadata ressursMessageMetadata,
    MessageMetadata orgtilknytningMessageMetadata
) {}
