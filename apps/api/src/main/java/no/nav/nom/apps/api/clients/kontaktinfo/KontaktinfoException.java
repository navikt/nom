package no.nav.nom.apps.api.clients.kontaktinfo;


import no.nav.nom.apps.gql.ErrorCode;

public class KontaktinfoException extends RuntimeException {

    private final ErrorCode errorCode;

    public KontaktinfoException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
