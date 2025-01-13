package no.nav.nom.apps.api.orgtilknytning.api.exceptions;

public class InvalidOrgtilknytningSektorException extends RuntimeException {
    public InvalidOrgtilknytningSektorException(String message) {
        super(message);
    }

    public static InvalidOrgtilknytningSektorException sektorIsNull(String orgEnhetId) {
        return new InvalidOrgtilknytningSektorException(String.format("Sektor er ikke satt på Orgtilknytning %s", orgEnhetId));
    }

    public static InvalidOrgtilknytningSektorException sektorCannotBeChanged() {
        return new InvalidOrgtilknytningSektorException("Sektor kan ikke endres når den allerede er satt på eksisterende Orgtilknytninger");
    }
}
