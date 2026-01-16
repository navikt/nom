package no.nav.nom.apps.api.ressurs.api.exceptions;

public class FysiskBrukerstedNotFoundException extends RuntimeException {
    public FysiskBrukerstedNotFoundException(String message) {
        super(message);
    }

    public static void fysiskBrukerstedIsNull(String visningnavn, String navident) {
        throw new FysiskBrukerstedNotFoundException(String.format("Ressursen %s (%s) mangler fysisk brukersted", visningnavn, navident));
    }
}
