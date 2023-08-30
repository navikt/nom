package no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands;

public record OppdaterPersonidentCommand(
    String fid,
    String personident
) {}
