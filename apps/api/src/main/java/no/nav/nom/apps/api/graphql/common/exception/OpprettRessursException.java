package no.nav.nom.apps.api.graphql.common.exception;

import no.nav.nom.apps.api.graphql.common.ErrorCode;

public class OpprettRessursException extends RuntimeException {

    private final ErrorCode errorCode;
    public OpprettRessursException(String message, Throwable cause, ErrorCode code) {
        super(message, cause);
        this.errorCode = code;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
