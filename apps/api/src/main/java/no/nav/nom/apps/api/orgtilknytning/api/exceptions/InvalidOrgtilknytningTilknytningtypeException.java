package no.nav.nom.apps.api.orgtilknytning.api.exceptions;

public class InvalidOrgtilknytningTilknytningtypeException extends RuntimeException {
    public InvalidOrgtilknytningTilknytningtypeException(String message) {
        super(message);
    }

    public static InvalidOrgtilknytningTilknytningtypeException tilknytningtypeIsNull(String orgEnhetId) {
        return new InvalidOrgtilknytningTilknytningtypeException(String.format("Tilknytningstype er ikke satt på Orgtilknytning %s", orgEnhetId));
    }

    public static InvalidOrgtilknytningTilknytningtypeException tilknytningtypeCannotBeChanged() {
        return new InvalidOrgtilknytningTilknytningtypeException("Tilknytningstype kan ikke endres når den allerede er satt på eksisterende Orgtilknytninger");
    }
}
