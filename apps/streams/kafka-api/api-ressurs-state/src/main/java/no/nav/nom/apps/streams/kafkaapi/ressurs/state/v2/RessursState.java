package no.nav.nom.apps.streams.kafkaapi.ressurs.state.v2;

import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record RessursState(
        String personident,
        String navident,
        Visningsnavn visningsnavn,
        Sektor sektor,
        Optional<List<Orgtilknytning>> orgtilknytninger,
        Optional<LocalDate> startdato,
        Optional<LocalDate> sluttdato
) { }
