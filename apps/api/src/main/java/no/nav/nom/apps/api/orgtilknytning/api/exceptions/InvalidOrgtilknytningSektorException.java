package no.nav.nom.apps.api.orgtilknytning.api.exceptions;

import no.nav.nom.contract.nom.Sektor;

public class InvalidOrgtilknytningSektorException extends RuntimeException {
    public InvalidOrgtilknytningSektorException(String message) {
        super(message);
    }

    public static InvalidOrgtilknytningSektorException sektorIsNull(String orgEnhetId) {
        return new InvalidOrgtilknytningSektorException(String.format("Sektor er ikke satt på Orgtilknytning %s", orgEnhetId));
    }

    public static InvalidOrgtilknytningSektorException sektorIsNullOrInvalidForFidOrNavident(Sektor sektor, String otFidOrNavident) {
        return new InvalidOrgtilknytningSektorException(String.format("Sektor (%s) er ikke satt eller er ugyldig på Orgtilknytning/Navident %s", sektor != null ? sektor.toString() : "mangler", otFidOrNavident));
    }

    public static InvalidOrgtilknytningSektorException sektorCannotBeChanged() {
        return new InvalidOrgtilknytningSektorException("Sektor kan ikke endres når den allerede er satt på eksisterende Orgtilknytninger");
    }
}
