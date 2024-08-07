package no.nav.nom.apps.api.ressurs.api.exceptions;

public class NomMasteredException extends RuntimeException {

    public NomMasteredException(String message) {
        super(message);
    }

    public static NomMasteredException actionNotAllowedForNonNomMastered() {
        return new NomMasteredException("Handling ikke tillatt for ressurser som ikke er under NOM-styring");
    }
}
