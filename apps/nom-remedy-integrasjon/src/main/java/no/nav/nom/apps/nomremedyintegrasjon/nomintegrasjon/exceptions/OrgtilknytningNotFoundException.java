package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions;

public class OrgtilknytningNotFoundException extends RuntimeException {

    public OrgtilknytningNotFoundException(String message) {
        super(message);
    }

    public static OrgtilknytningNotFoundException orgtilknytningNotFoundException(String navident) {
        return new OrgtilknytningNotFoundException(String.format("Fant ingen aktive eller fremtidige DOL orgtilknytninger for navident '%s'", navident));
    }
}
