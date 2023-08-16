package no.nav.nom.contract.nom.state;

import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.util.Optional;

public record RessursState(
    String fid,
    String personident,
    String navident,
    Sektor sektor,
    Visningsnavn visningsnavn,
    Optional<LocalDate> sluttdato
) {}
