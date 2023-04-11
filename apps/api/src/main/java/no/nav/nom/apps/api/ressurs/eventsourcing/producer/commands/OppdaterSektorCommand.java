package no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.Sektor;

public record OppdaterSektorCommand(
    String fid,
    Sektor sektor
) {}
