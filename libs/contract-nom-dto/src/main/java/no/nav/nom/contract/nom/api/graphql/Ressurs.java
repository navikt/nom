package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder(toBuilder = true)
public class Ressurs {
    @Deprecated    String navIdent;
    @Deprecated    String personIdent;
    @Deprecated    String visningsNavn;
    @Deprecated    List<Sektor> sektor;

    String navident;
    String personident;
    LocalDate sluttdato;
    LocalDate startdato;
    Sektor gjeldendeSektor;
    IdentType identType;
    String fornavn;
    String etternavn;
    String visningsnavn;
    String primaryTelefon;
    List<Telefon> telefon;
    Tilknytningtype gjeldendeTilknytningtype;
}