package no.nav.nom.apps.api.graphql.common.exception;

import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.contract.ad.AdRessurser;
import org.springframework.http.ResponseEntity;

public class ADResponseException extends RuntimeException {

    private final ResponseEntity<AdRessurser> response;
    private final ErrorCode errorCode;

    public ADResponseException(String message, ResponseEntity<AdRessurser> response, ErrorCode errorCode) {
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
