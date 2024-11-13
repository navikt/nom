package no.nav.nom.apps.api.ressurs.api.exceptions;

import no.nav.nom.contract.nom.Sektor;

public class InvalidSektorException extends RuntimeException {

    public InvalidSektorException(String message) {
        super(message);
    }

    public static InvalidSektorException actionNotAllowedForSektor(Sektor sektor) {
        return new InvalidSektorException(
            String.format("Handling ikke tillatt for ressurser i sektor %s", sektor)
        );
    }

    public static InvalidSektorException sektorIsNull() {
        return new InvalidSektorException("Sektor kan ikke være null på nye Orgtilknytninger");
    }

    public static InvalidSektorException sektorCannotBeChanged() {
        return new InvalidSektorException("Sektor kan ikke endres på eksisterende Orgtilknytninger");
    }
}
