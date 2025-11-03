package no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.Tilknytningtype;

public record OppdaterTilknytningtypeCommand(
    String fid,
    Tilknytningtype tilknytningtype
) {}
