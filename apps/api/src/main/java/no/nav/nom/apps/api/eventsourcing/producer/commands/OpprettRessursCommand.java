package no.nav.nom.apps.api.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

public record OpprettRessursCommand(
        String personident,
        String navident,
        Sektor sektor,
        Visningsnavn visningsnavn,
        InputMetadata inputMetadata
) implements Command { }
