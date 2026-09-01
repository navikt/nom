package no.nav.nom.apps.api.ressurs.api.app;

import java.time.LocalDate;

public record RessurserByFysiskBrukerstedAndTilgangsenhetIdExcelResponse(
    String navident,
    String fornavn,
    String mellomnavn,
    String etternavn,
    String orgEnhetId,
    String orgEnhetNavn,
    String tilgangsenhetId,
    String fysiskBrukerstedNr,
    String fysiskBrukerstedNavn,
    String telefonnummer,
    String sektor,
    String tilknytningsType,
    LocalDate startdato,
    String epost,
    String lederNavident,
    String lederNavn
) {
}
