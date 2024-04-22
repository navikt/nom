package no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands;

public record OppdaterPrimaryTelefonnummerCommand(
    String fid,
    String primaryTelefonnummer
) {
}
