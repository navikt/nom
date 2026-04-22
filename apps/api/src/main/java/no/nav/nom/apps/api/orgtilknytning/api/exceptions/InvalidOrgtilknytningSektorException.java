package no.nav.nom.apps.api.orgtilknytning.api.exceptions;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.Sektor;

@Slf4j
public class InvalidOrgtilknytningSektorException extends RuntimeException {
    public InvalidOrgtilknytningSektorException(String message) {
        super(message);
    }

    public static InvalidOrgtilknytningSektorException sektorIsNull(String orgEnhetId) {
        String msg = "Sektor er ikke satt på Orgtilknytning %s".formatted(orgEnhetId);
        log.error(msg);
        return new InvalidOrgtilknytningSektorException(msg);
    }

    public static InvalidOrgtilknytningSektorException sektorIsNullOrInvalidForFidOrNavident(Sektor sektor, String otFidOrNavident) {
        String msg = "Sektor (%s) er ikke satt eller er ugyldig på Orgtilknytning/Navident %s".formatted(sektor != null ? sektor.toString() : "mangler", otFidOrNavident);
        log.error(msg);
        return new InvalidOrgtilknytningSektorException(msg);
    }

    public static InvalidOrgtilknytningSektorException sektorCannotBeChanged() {
        String msg = "Sektor kan ikke endres når den allerede er satt på eksisterende Orgtilknytninger";
        log.error(msg);
        return new InvalidOrgtilknytningSektorException(msg);
    }
}
