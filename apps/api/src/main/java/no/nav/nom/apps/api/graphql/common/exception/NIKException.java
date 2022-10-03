package no.nav.nom.apps.api.graphql.common.exception;

import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

public class NIKException extends RuntimeException {

    private final ResponseEntity<OpprettNavidentResponse> response;
    private final ErrorCode errorCode;
    public NIKException(String message, ResponseEntity response, ErrorCode errorCode) {
        super(message);
        this.response = response;
        this.errorCode = errorCode;
    }

    public ResponseEntity<?> getSource() {
        return response;
    }
//    public String getFeilmelding() {
//        return Optional.ofNullable(response.getBody()).map(OpprettNavidentResponse::getFeilmelding).orElse()
//    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
