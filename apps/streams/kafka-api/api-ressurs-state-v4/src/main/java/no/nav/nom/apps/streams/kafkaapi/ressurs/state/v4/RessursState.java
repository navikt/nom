package no.nav.nom.apps.streams.kafkaapi.ressurs.state.v4;

import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.util.List;

public record RessursState(
        String personident,
        String navident,
        Visningsnavn visningsnavn,
        LocalDate startdato,
        LocalDate sluttdato,
        Sektor sektor,
        IdentType identType,
        String primaryTelefonnummer,
        String primaryFysiskBrukersted,
        List<Orgtilknytning> orgtilknytninger,
        Tilknytningtype tilknytningtype
) { }
