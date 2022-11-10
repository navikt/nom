package no.nav.nom.contract.nom.state;

import no.nav.nom.contract.nom.eventsource.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public record RessursState(
        UUID fid,
        String personident,
        String navident,
        Sektor sektor,
        Visningsnavn visningsnavn,
        Optional<LocalDate> sluttdato
) { }
