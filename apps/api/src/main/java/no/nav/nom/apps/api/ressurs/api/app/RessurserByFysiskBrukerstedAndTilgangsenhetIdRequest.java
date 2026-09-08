package no.nav.nom.apps.api.ressurs.api.app;

public record RessurserByFysiskBrukerstedAndTilgangsenhetIdRequest(
    String fysiskBrukerstedNr,
    String tilgangsenhetId
) {
}
