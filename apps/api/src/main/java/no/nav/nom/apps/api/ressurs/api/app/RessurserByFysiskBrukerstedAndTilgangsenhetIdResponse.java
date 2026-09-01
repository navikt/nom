package no.nav.nom.apps.api.ressurs.api.app;

import no.nav.nom.contract.nom.eventsource.Visningsnavn;

public record RessurserByFysiskBrukerstedAndTilgangsenhetIdResponse(
    String navident,
    Visningsnavn visningsnavn,
    String orgEnhetId,
    String orgEnhetNavn,
    String tilgangsenhetId,
    String fysiskBrukerstedNummer,
    String fysiskBrukerstedNavn
) {
}
