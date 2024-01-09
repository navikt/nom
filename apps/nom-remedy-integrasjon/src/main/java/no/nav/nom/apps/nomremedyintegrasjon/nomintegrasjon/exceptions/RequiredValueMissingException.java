package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions;

public class RequiredValueMissingException extends RuntimeException{

    public RequiredValueMissingException(String message) {
        super(message);
    }

    public static RequiredValueMissingException forField(String fieldName) {
        return new RequiredValueMissingException(String.format("Value for field '%s' missing", fieldName));
    }
}
