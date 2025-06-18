package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions;

public class RessursNotFoundException extends RuntimeException {
    public RessursNotFoundException(String message) {
        super(message);
    }

    public static RessursNotFoundException notFound(String navident, String orgtilknytningFid) {
        return new RessursNotFoundException(String.format("Ressurs med navident '%s' ble ikke funnet for %s", navident, orgtilknytningFid));
    }

    public static RessursNotFoundException notFound(String navident) {
        return new RessursNotFoundException(String.format("Ressurs med navident '%s' ble ikke funnet", navident));
    }
}
