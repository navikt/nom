package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

public record MessageMetadata(
        long offset,
        long timestamp
) {}
