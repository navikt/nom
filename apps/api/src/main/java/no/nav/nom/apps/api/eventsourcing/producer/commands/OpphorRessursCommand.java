package no.nav.nom.apps.api.eventsourcing.producer.commands;

import java.time.LocalDate;

public record OpphorRessursCommand(
        String fid,
        LocalDate sluttdato
) implements Command { }
