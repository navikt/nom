package no.nav.nom.apps.api.graphql.common.exception;

import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import org.springframework.http.ResponseEntity;

public class NIKException extends RuntimeException {

    private final ResponseEntity<OpprettNavidentResponse> response;
    private final ErrorCode errorCode;

    public NIKException(String message, ResponseEntity<OpprettNavidentResponse> response, ErrorCode errorCode) {
        super(message);
        this.response = response;
        this.errorCode = errorCode;
    }

    public ResponseEntity<?> getSource() {
        return response;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
