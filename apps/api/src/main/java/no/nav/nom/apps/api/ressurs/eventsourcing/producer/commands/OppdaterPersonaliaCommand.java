package no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.Visningsnavn;

public record OppdaterPersonaliaCommand(
        String fid,
        Visningsnavn visningsnavn
) { }
