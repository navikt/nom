package no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands;

public record SlettRessursCommand(
    String fid
) implements RessursCommand {}
