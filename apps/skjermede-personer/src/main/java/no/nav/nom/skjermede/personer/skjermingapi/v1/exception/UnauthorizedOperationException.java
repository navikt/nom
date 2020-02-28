package no.nav.nom.skjermede.personer.skjermingapi.v1.exception;

public class UnauthorizedOperationException extends RuntimeException {

    public UnauthorizedOperationException(String message) {
        super(message);
    }
}
