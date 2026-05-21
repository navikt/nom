package no.nav.nom.apps.api.tilgangsenhet;

public record Tilgangsenhet(
    String enhetId,
    String navn
) {
    public static Tilgangsenhet fromNorgEnhet(NorgService.NorgEnhet norgEnhet) {
        return new Tilgangsenhet(norgEnhet.enhetNr(), norgEnhet.navn());
    }
}
