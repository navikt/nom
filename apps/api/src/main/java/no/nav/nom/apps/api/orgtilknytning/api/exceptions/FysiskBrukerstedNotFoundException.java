package no.nav.nom.apps.api.orgtilknytning.api.exceptions;

public class FysiskBrukerstedNotFoundException extends RuntimeException {
    public FysiskBrukerstedNotFoundException(String message) {
        super(message);
    }

    public static FysiskBrukerstedNotFoundException notFound(String fysiskBrukerstedNr) {
        return new FysiskBrukerstedNotFoundException(
            "Fant ikke fysisk brukersted med nr: %s".formatted(fysiskBrukerstedNr)
        );
    }
}
