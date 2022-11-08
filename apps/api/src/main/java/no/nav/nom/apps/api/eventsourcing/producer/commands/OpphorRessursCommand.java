package no.nav.nom.apps.api.eventsourcing.producer.commands;

import java.time.LocalDate;
import java.util.UUID;

public record OpphorRessursCommand(
        UUID fid,
        LocalDate sluttdato
) implements Command { }
