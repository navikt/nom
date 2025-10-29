package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;
import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class Ressurs {
    @Deprecated    String navIdent;
    @Deprecated    String personIdent;
    @Deprecated    String visningsNavn;
    @Deprecated    List<Sektor> sektor;
    @Deprecated    List<Sektor> ressurstype;

    String navident;
    String personident;
    LocalDate sluttdato;
    LocalDate startdato;
    Sektor gjeldendeSektor;
    String fornavn;
    String etternavn;
    String visningsnavn;
    String primaryTelefon;
}