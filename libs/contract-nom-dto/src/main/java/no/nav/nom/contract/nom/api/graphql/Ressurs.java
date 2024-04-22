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
    UUID nomId;
    String navIdent;
    String navident;
    String personIdent;
    String personident;
    List<Sektor> ressurstype;
    List<Sektor> sektor;
    LocalDate sluttdato;
    LocalDate startdato;
    String fornavn;
    String etternavn;
    String visningsNavn;
    String visningsnavn;
    String primaryTelefon;
}