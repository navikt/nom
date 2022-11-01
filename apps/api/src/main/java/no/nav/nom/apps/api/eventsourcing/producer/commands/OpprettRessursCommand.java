package no.nav.nom.apps.api.eventsourcing.producer.commands;

import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;

public record OpprettRessursCommand(
        String personident,
        String navident,
        RessursOpprettetEventV1.RessursRegisteredEventData.Sektor sektor,
        RessursOpprettetEventV1.RessursRegisteredEventData.Visningsnavn visningsnavn
) implements Command { }
