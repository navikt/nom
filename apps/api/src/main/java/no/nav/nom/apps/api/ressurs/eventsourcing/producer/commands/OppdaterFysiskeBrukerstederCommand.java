package no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands;

import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.util.Set;

@Builder(toBuilder = true)
public record OppdaterFysiskeBrukerstederCommand(
    String fid,
    Set<String> fysiskeBrukersteder,
    String primaryFysiskBrukersted
) {}
