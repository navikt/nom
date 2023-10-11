package no.nav.nom.apps.api.rest.exceptions;

public class RessursNotFoundException extends RuntimeException{

    public RessursNotFoundException(String message) {
        super(message);
    }

    public static RessursNotFoundException navident(String navident) {
        return new RessursNotFoundException(String.format("Kunne ikke finne ressurs med navident %s", navident));
    }
}
