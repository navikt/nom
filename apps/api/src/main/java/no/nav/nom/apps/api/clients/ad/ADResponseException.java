package no.nav.nom.apps.api.clients.ad;

import no.nav.nom.libs.gql.ErrorCode;
import no.nav.nom.contract.ad.AdRessurser;
import org.springframework.http.ResponseEntity;

public class ADResponseException extends RuntimeException {

    private final ErrorCode errorCode;

    public ADResponseException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
