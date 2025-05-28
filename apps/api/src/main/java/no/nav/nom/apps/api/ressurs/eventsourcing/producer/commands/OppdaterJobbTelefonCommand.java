package no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.Jobbtelefon;

public record OppdaterJobbTelefonCommand(
    String fid,
    String navident,
    Jobbtelefon jobbtelefon
) {
}
